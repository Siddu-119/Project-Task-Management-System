package com.projectmgmt.service;

import com.projectmgmt.dto.MemberResponse;
import com.projectmgmt.model.*;
import com.projectmgmt.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectMemberService {

    @Autowired
    private ProjectMemberRepository memberRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private AccessService access;
    @Autowired
    private NotificationService notifications;

    public List<MemberResponse> list(Long projectId) {
        access.requireProjectAccess(projectId);
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        return memberRepository.findByProjectId(projectId).stream().map(m -> {
            User u = m.getUser();
            MemberResponse r = new MemberResponse();
            r.setUserId(u.getId());
            r.setName(u.getName());
            r.setEmail(u.getEmail());
            r.setRole(u.getRole().name());
            r.setAddedAt(m.getAddedAt());
            for (Task t : tasks) {
                if (t.getAssignedTo() == null || !t.getAssignedTo().getId().equals(u.getId())) continue;
                r.setTotalTasks(r.getTotalTasks() + 1);
                if (t.getStatus() == Task.Status.COMPLETED) r.setCompletedTasks(r.getCompletedTasks() + 1);
                else r.setOpenTasks(r.getOpenTasks() + 1);
                if (TaskService.isOverdue(t)) r.setOverdueTasks(r.getOverdueTasks() + 1);
            }
            return r;
        }).collect(Collectors.toList());
    }

    public void add(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (memberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new IllegalArgumentException(user.getName() + " is already a member of this project.");
        }
        ProjectMember m = new ProjectMember();
        m.setProject(project);
        m.setUser(user);
        memberRepository.save(m);
        if (!user.getId().equals(access.me().id())) {
            notifications.notify(user, "ADDED_TO_PROJECT",
                    "You were added to the project \"" + project.getName() + "\"", null, projectId);
        }
    }

    /** Removing a member also un-assigns their tasks in this project. */
    public void remove(Long projectId, Long userId) {
        ProjectMember m = memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found in this project"));
        for (Task t : taskRepository.findByProjectId(projectId)) {
            if (t.getAssignedTo() != null && t.getAssignedTo().getId().equals(userId)) {
                t.setAssignedTo(null);
                taskRepository.save(t);
            }
        }
        memberRepository.delete(m);
    }
}
