package com.projectmgmt.service;

import com.projectmgmt.dto.ProjectResponse;
import com.projectmgmt.model.Project;
import com.projectmgmt.model.Task;
import com.projectmgmt.model.User;
import com.projectmgmt.repository.TaskRepository;
import com.projectmgmt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Dashboard and Reports - every number is calculated from the database on each request. */
@Service
public class DashboardService {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private AccessService access;

    private long count(List<Task> tasks, Task.Status s) {
        return tasks.stream().filter(t -> t.getStatus() == s).count();
    }

    private Map<String, Object> summary(List<ProjectResponse> projects, List<Task> tasks) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalProjects", projects.size());
        m.put("activeProjects", projects.stream().filter(p -> "ACTIVE".equals(p.getStatus())).count());
        m.put("completedProjects", projects.stream().filter(p -> "COMPLETED".equals(p.getStatus())).count());
        m.put("totalTasks", tasks.size());
        m.put("pendingTasks", count(tasks, Task.Status.TODO));
        m.put("inProgressTasks", count(tasks, Task.Status.IN_PROGRESS));
        m.put("reviewTasks", count(tasks, Task.Status.REVIEW));
        m.put("completedTasks", count(tasks, Task.Status.COMPLETED));
        m.put("overdueTasks", tasks.stream().filter(TaskService::isOverdue).count());
        return m;
    }

    private List<Map<String, Object>> progressList(List<ProjectResponse> projects) {
        return projects.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("status", p.getStatus());
            m.put("progress", p.getProgress());
            m.put("totalTasks", p.getTotalTasks());
            m.put("completedTasks", p.getCompletedTasks());
            return m;
        }).collect(Collectors.toList());
    }

    /** Admin: whole system. Employee: their projects and the tasks assigned to them. */
    public Map<String, Object> dashboard() {
        boolean admin = access.me().isAdmin();
        List<ProjectResponse> projects = projectService.buildResponses(projectService.visibleProjects());
        List<Task> tasks = admin ? taskRepository.findAll() : taskRepository.findByAssignedToId(access.me().id());

        Map<String, Object> m = summary(projects, tasks);
        m.put("scope", admin ? "ALL" : "MINE");
        m.put("recentProjects", projects.stream()
                .sorted(Comparator.comparing(ProjectResponse::getCreatedAt,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())).reversed())
                .limit(5).collect(Collectors.toList()));
        m.put("recentTasks", tasks.stream()
                .sorted(Comparator.comparing(Task::getUpdatedAt,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())).reversed())
                .limit(5).map(taskService::toResponse).collect(Collectors.toList()));
        m.put("projectProgress", progressList(projects));
        return m;
    }

    /** Reports are admin-only (see SecurityConfig). */
    public Map<String, Object> reports() {
        List<ProjectResponse> projects = projectService.buildResponses(projectService.visibleProjects());
        List<Task> tasks = taskRepository.findAll();

        Map<String, Object> m = summary(projects, tasks);
        List<Map<String, Object>> members = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            List<Task> mine = tasks.stream()
                    .filter(t -> t.getAssignedTo() != null && t.getAssignedTo().getId().equals(u.getId()))
                    .collect(Collectors.toList());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", u.getId());
            row.put("name", u.getName());
            row.put("role", u.getRole().name());
            row.put("total", mine.size());
            row.put("todo", count(mine, Task.Status.TODO));
            row.put("inProgress", count(mine, Task.Status.IN_PROGRESS));
            row.put("review", count(mine, Task.Status.REVIEW));
            row.put("completed", count(mine, Task.Status.COMPLETED));
            row.put("overdue", mine.stream().filter(TaskService::isOverdue).count());
            members.add(row);
        }
        m.put("memberStats", members);
        m.put("projectProgress", progressList(projects));
        return m;
    }
}
