package com.projectmgmt.service;

import com.projectmgmt.dto.ProjectRequest;
import com.projectmgmt.dto.ProjectResponse;
import com.projectmgmt.model.Project;
import com.projectmgmt.model.Task;
import com.projectmgmt.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ProjectMemberRepository memberRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private AccessService access;

    /** Progress = completed tasks / total tasks. Calculated every time, never stored. */
    public ProjectResponse toResponse(Project p, List<Task> tasks) {
        ProjectResponse r = new ProjectResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setDescription(p.getDescription());
        r.setStartDate(p.getStartDate());
        r.setEndDate(p.getEndDate());
        r.setStatus(p.getStatus().name());
        r.setCreatedAt(p.getCreatedAt());
        r.setTotalTasks(tasks.size());
        for (Task t : tasks) {
            switch (t.getStatus()) {
                case TODO -> r.setTodoTasks(r.getTodoTasks() + 1);
                case IN_PROGRESS -> r.setInProgressTasks(r.getInProgressTasks() + 1);
                case REVIEW -> r.setReviewTasks(r.getReviewTasks() + 1);
                case COMPLETED -> r.setCompletedTasks(r.getCompletedTasks() + 1);
            }
            if (TaskService.isOverdue(t)) r.setOverdueTasks(r.getOverdueTasks() + 1);
        }
        r.setProgress(tasks.isEmpty() ? 0 : Math.round(r.getCompletedTasks() * 100f / tasks.size()));
        r.setMemberCount(memberRepository.countByProjectId(p.getId()));
        return r;
    }

    public List<Project> visibleProjects() {
        if (access.me().isAdmin()) return projectRepository.findAll();
        Set<Long> ids = access.myProjectIds();
        return ids.isEmpty() ? new ArrayList<>() : projectRepository.findAllById(ids);
    }

    public List<ProjectResponse> search(String search, String status) {
        List<Project> projects = visibleProjects().stream()
                .filter(p -> search == null || search.isBlank()
                        || p.getName().toLowerCase().contains(search.trim().toLowerCase()))
                .filter(p -> status == null || status.isBlank() || p.getStatus().name().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(Project::getId).reversed())
                .collect(Collectors.toList());
        return buildResponses(projects);
    }

    public List<ProjectResponse> buildResponses(List<Project> projects) {
        if (projects.isEmpty()) return new ArrayList<>();
        List<Long> ids = projects.stream().map(Project::getId).collect(Collectors.toList());
        Map<Long, List<Task>> byProject = taskRepository.findByProjectIdIn(ids).stream()
                .collect(Collectors.groupingBy(t -> t.getProject().getId()));
        return projects.stream()
                .map(p -> toResponse(p, byProject.getOrDefault(p.getId(), new ArrayList<>())))
                .collect(Collectors.toList());
    }

    private Project find(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
    }

    public ProjectResponse getProjectById(Long id) {
        Project p = find(id);
        access.requireProjectAccess(id);
        return toResponse(p, taskRepository.findByProjectId(id));
    }

    private void apply(Project p, ProjectRequest req) {
        if (req.getStartDate() != null && req.getEndDate() != null && req.getEndDate().isBefore(req.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before the start date");
        }
        Project.Status status = Project.Status.PLANNING;
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            try { status = Project.Status.valueOf(req.getStatus().trim().toUpperCase()); }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Status must be PLANNING, ACTIVE, ON_HOLD or COMPLETED");
            }
        }
        p.setName(req.getName().trim());
        p.setDescription(req.getDescription());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setStatus(status);
    }

    public ProjectResponse createProject(ProjectRequest req) {
        Project p = new Project();
        apply(p, req);
        Project saved = projectRepository.save(p);
        return toResponse(saved, new ArrayList<>());
    }

    public ProjectResponse updateProject(Long id, ProjectRequest req) {
        Project p = find(id);
        apply(p, req);
        Project saved = projectRepository.save(p);
        return toResponse(saved, taskRepository.findByProjectId(id));
    }

    /** Deleting a project also removes its tasks, comments and memberships (no orphans left behind). */
    public void deleteProject(Long id) {
        find(id);
        commentRepository.deleteByTaskProjectId(id);
        taskRepository.deleteByProjectId(id);
        memberRepository.deleteByProjectId(id);
        projectRepository.deleteById(id);
    }

    /** Recent activity for the project page, built from tasks, comments and memberships. */
    public List<Map<String, Object>> activity(Long id) {
        find(id);
        access.requireProjectAccess(id);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Task t : taskRepository.findByProjectId(id)) {
            String who = t.getAssignedTo() != null ? " (" + t.getAssignedTo().getName() + ")" : "";
            items.add(activityItem("TASK", "Task \"" + t.getTitle() + "\"" + who + " is "
                    + t.getStatus().name().replace('_', ' '), t.getUpdatedAt()));
        }
        commentRepository.findTop10ByTaskProjectIdOrderByCreatedAtDesc(id).forEach(c ->
            items.add(activityItem("COMMENT", c.getUser().getName() + " commented on \""
                    + c.getTask().getTitle() + "\"", c.getCreatedAt())));
        memberRepository.findByProjectId(id).forEach(m ->
            items.add(activityItem("MEMBER", m.getUser().getName() + " is on the team", m.getAddedAt())));
        items.sort(Comparator.comparing((Map<String, Object> m) -> (LocalDateTime) m.get("time"),
                Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())).reversed());
        return items.size() > 10 ? items.subList(0, 10) : items;
    }

    private Map<String, Object> activityItem(String type, String message, LocalDateTime time) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("message", message);
        m.put("time", time);
        return m;
    }
}
