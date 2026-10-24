package com.projectmgmt.service;

import com.projectmgmt.dto.RecommendationRequest;
import com.projectmgmt.dto.RecommendationResponse;
import com.projectmgmt.logic.RecommendationEngine;
import com.projectmgmt.logic.RecommendationEngine.Candidate;
import com.projectmgmt.logic.RecommendationEngine.TaskNeed;
import com.projectmgmt.logic.SkillNames;
import com.projectmgmt.model.ProjectMember;
import com.projectmgmt.model.Task;
import com.projectmgmt.model.User;
import com.projectmgmt.repository.ProjectMemberRepository;
import com.projectmgmt.repository.ProjectRepository;
import com.projectmgmt.repository.TaskRepository;
import com.projectmgmt.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Gathers real data (project members, their open tasks and skills) and hands it to the rule-based engine.
 * READ-ONLY: this class never saves or assigns anything - an admin confirms by saving the task.
 */
@Service
public class RecommendationService {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectMemberRepository memberRepository;
    @Autowired
    private TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public RecommendationResponse recommend(RecommendationRequest req) {
        if (!CurrentUser.get().isAdmin()) {
            throw new SecurityException("Only admins can ask for assignee recommendations.");
        }
        projectRepository.findById(req.getProjectId()).orElseThrow(() -> new RuntimeException("Project not found"));

        Task.Priority priority = Task.Priority.MEDIUM;
        if (req.getPriority() != null && !req.getPriority().isBlank()) {
            try { priority = Task.Priority.valueOf(req.getPriority().trim().toUpperCase()); }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Priority must be LOW, MEDIUM, HIGH or URGENT");
            }
        }
        List<String> requiredSkills = SkillNames.normalize(req.getSkills());
        LocalDate today = LocalDate.now();

        Long currentAssigneeId = null;
        if (req.getTaskId() != null) {
            Task existing = taskRepository.findById(req.getTaskId()).orElse(null);
            if (existing != null && existing.getAssignedTo() != null) currentAssigneeId = existing.getAssignedTo().getId();
        }

        List<Candidate> candidates = new ArrayList<>();
        for (ProjectMember m : memberRepository.findByProjectId(req.getProjectId())) {
            User u = m.getUser();
            int active = 0, overdue = 0;
            double openHours = 0;
            for (Task t : taskRepository.findByAssignedToId(u.getId())) {
                if (t.getStatus() == Task.Status.COMPLETED) continue;
                if (req.getTaskId() != null && req.getTaskId().equals(t.getId())) continue;   // the task being edited
                active++;
                if (TaskService.isOverdue(t)) overdue++;
                openHours += t.getEstimatedHours() != null ? t.getEstimatedHours() : RecommendationEngine.DEFAULT_TASK_HOURS;
            }
            candidates.add(new Candidate(u.getId(), u.getName(), SkillService.names(u.getSkills()),
                    active, overdue, openHours, u.getId().equals(currentAssigneeId)));
        }

        TaskNeed need = new TaskNeed(requiredSkills, priority.name(), req.getDeadline(), req.getEstimatedHours(), today);
        return RecommendationResponse.of(RecommendationEngine.recommend(need, candidates));
    }
}
