package com.projectmgmt.service;

import com.projectmgmt.logic.ProjectRiskCalculator;
import com.projectmgmt.logic.RiskResult;
import com.projectmgmt.logic.TaskRiskCalculator;
import com.projectmgmt.model.Project;
import com.projectmgmt.model.Task;
import com.projectmgmt.repository.ProjectRepository;
import com.projectmgmt.repository.TaskRepository;
import com.projectmgmt.security.AuthUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Collects real project/task data and passes it to the rule-based risk calculators. Read-only. */
@Service
public class RiskService {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private AccessService access;

    /** Any project member (or admin). The result only contains project-level counts, nothing about individuals. */
    public RiskResult projectRisk(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        access.requireProjectAccess(projectId);

        List<Task> tasks = taskRepository.findByProjectId(projectId);
        LocalDateTime since = LocalDateTime.now().minusDays(14);
        int completed = 0, overdue = 0, unassigned = 0, last14 = 0, noEstimate = 0;
        double remainingHours = 0;
        Set<Long> assignees = new HashSet<>();

        for (Task t : tasks) {
            if (t.getStatus() == Task.Status.COMPLETED) {
                completed++;
                if (t.getUpdatedAt() != null && t.getUpdatedAt().isAfter(since)) last14++;
                continue;
            }
            if (TaskService.isOverdue(t)) overdue++;
            if (t.getAssignedTo() == null) unassigned++; else assignees.add(t.getAssignedTo().getId());
            if (t.getEstimatedHours() == null) noEstimate++; else remainingHours += t.getEstimatedHours();
        }

        var input = new ProjectRiskCalculator.Input(LocalDate.now(), project.getStartDate(), project.getEndDate(),
                project.getStatus().name(), tasks.size(), completed, overdue, unassigned, last14,
                remainingHours, noEstimate, assignees.size());
        return ProjectRiskCalculator.calculate(input)
                .withNote("Completion time is approximated by each completed task's last update.");
    }

    /** Admin or the assigned person only: the reasons mention that person's workload. */
    public RiskResult taskRisk(Long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        AuthUser me = access.me();
        boolean assignee = task.getAssignedTo() != null && me.id().equals(task.getAssignedTo().getId());
        if (!me.isAdmin() && !assignee) {
            throw new SecurityException("Only admins and the assigned person can see a task's risk details.");
        }

        int active = 0, overdue = 0;
        if (task.getAssignedTo() != null) {
            for (Task t : taskRepository.findByAssignedToId(task.getAssignedTo().getId())) {
                if (t.getStatus() == Task.Status.COMPLETED) continue;
                active++;
                if (TaskService.isOverdue(t)) overdue++;
            }
        }
        var input = new TaskRiskCalculator.Input(LocalDate.now(), task.getStatus().name(), task.getPriority().name(),
              task.getDeadline(),
task.getEstimatedHours() == null ? null : Double.valueOf(task.getEstimatedHours()),
task.getAssignedTo() != null, active, overdue);
        return TaskRiskCalculator.calculate(input);
    }
}
