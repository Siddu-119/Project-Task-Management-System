package com.projectmgmt.service;

import com.projectmgmt.dto.TaskRequest;
import com.projectmgmt.dto.TaskResponse;
import com.projectmgmt.model.Project;
import com.projectmgmt.model.Task;
import com.projectmgmt.model.User;
import com.projectmgmt.repository.*;
import com.projectmgmt.security.AuthUser;
import com.projectmgmt.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectMemberRepository memberRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private AccessService access;
    @Autowired
    private NotificationService notifications;

    // ---------- helpers ----------

    public static boolean isOverdue(Task t) {
        return t.getDeadline() != null && t.getDeadline().isBefore(LocalDate.now())
                && t.getStatus() != Task.Status.COMPLETED;
    }

    public TaskResponse toResponse(Task task) {
        TaskResponse r = new TaskResponse();
        r.setId(task.getId());
        r.setTitle(task.getTitle());
        r.setDescription(task.getDescription());
        r.setStatus(task.getStatus().name());
        r.setPriority(task.getPriority().name());
        r.setDeadline(task.getDeadline());
        r.setOverdue(isOverdue(task));
        r.setCreatedAt(task.getCreatedAt());
        r.setUpdatedAt(task.getUpdatedAt());
        if (task.getProject() != null) {
            r.setProjectId(task.getProject().getId());
            r.setProjectName(task.getProject().getName());
        }
        if (task.getAssignedTo() != null) {
            r.setAssignedToId(task.getAssignedTo().getId());
            r.setAssignedToName(task.getAssignedTo().getName());
        }
        return r;
    }

    private Task.Status parseStatus(String value) {
        if (value == null || value.isBlank()) return Task.Status.TODO;
        try { return Task.Status.valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status must be TODO, IN_PROGRESS, REVIEW or COMPLETED");
        }
    }

    private Task.Priority parsePriority(String value) {
        if (value == null || value.isBlank()) return Task.Priority.MEDIUM;
        try { return Task.Priority.valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Priority must be LOW, MEDIUM, HIGH or URGENT");
        }
    }

    /** Tasks the logged-in user may see: admin = all, employee = tasks of their projects + tasks assigned to them. */
    private List<Task> visibleTasks() {
        AuthUser me = access.me();
        if (me.isAdmin()) return taskRepository.findAll();
        Map<Long, Task> result = new LinkedHashMap<>();
        Set<Long> projectIds = access.myProjectIds();
        if (!projectIds.isEmpty()) {
            taskRepository.findByProjectIdIn(projectIds).forEach(t -> result.put(t.getId(), t));
        }
        taskRepository.findByAssignedToId(me.id()).forEach(t -> result.put(t.getId(), t));
        return new ArrayList<>(result.values());
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
    }

    private void requireTaskAccess(Task task) {
        AuthUser me = access.me();
        if (me.isAdmin()) return;
        boolean assignee = task.getAssignedTo() != null && me.id().equals(task.getAssignedTo().getId());
        boolean member = task.getProject() != null && access.canAccessProject(task.getProject().getId());
        if (!assignee && !member) throw new SecurityException("You cannot access this task.");
    }

    // ---------- reading ----------

    public List<TaskResponse> search(String search, String status, String priority, Long assignedToId,
                                     Long projectId, LocalDate dueFrom, LocalDate dueTo, Boolean overdue) {
        return visibleTasks().stream()
                .filter(t -> search == null || search.isBlank()
                        || t.getTitle().toLowerCase().contains(search.trim().toLowerCase()))
                .filter(t -> status == null || status.isBlank() || t.getStatus().name().equalsIgnoreCase(status))
                .filter(t -> priority == null || priority.isBlank() || t.getPriority().name().equalsIgnoreCase(priority))
                .filter(t -> assignedToId == null
                        || (t.getAssignedTo() != null && t.getAssignedTo().getId().equals(assignedToId)))
                .filter(t -> projectId == null
                        || (t.getProject() != null && t.getProject().getId().equals(projectId)))
                .filter(t -> dueFrom == null || (t.getDeadline() != null && !t.getDeadline().isBefore(dueFrom)))
                .filter(t -> dueTo == null || (t.getDeadline() != null && !t.getDeadline().isAfter(dueTo)))
                .filter(t -> overdue == null || !overdue || isOverdue(t))
                .sorted(Comparator.comparing(Task::getId).reversed())
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<TaskResponse> getAllTasks() {
        return search(null, null, null, null, null, null, null, null);
    }

    public TaskResponse getTask(Long id) {
        Task task = findTask(id);
        requireTaskAccess(task);
        return toResponse(task);
    }

    public List<TaskResponse> getTasksByUser(Long userId) {
        AuthUser me = CurrentUser.get();
        if (!me.isAdmin() && !me.id().equals(userId)) {
            throw new SecurityException("You can only view your own tasks.");
        }
        return taskRepository.findByAssignedToId(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TaskResponse> getTasksByProject(Long projectId) {
        access.requireProjectAccess(projectId);
        return taskRepository.findByProjectId(projectId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ---------- writing (admin only - enforced in SecurityConfig) ----------

    private void applyRequest(Task task, TaskRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (request.getDeadline() != null && project.getStartDate() != null
                && request.getDeadline().isBefore(project.getStartDate())) {
            throw new IllegalArgumentException("Due date cannot be before the project start date ("
                    + project.getStartDate() + ")");
        }
        User assignee = null;
        if (request.getAssignedToId() != null) {
            assignee = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (!memberRepository.existsByProjectIdAndUserId(project.getId(), assignee.getId())) {
                throw new IllegalArgumentException(assignee.getName()
                        + " is not a member of this project. Add them to the project team first.");
            }
        }
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setStatus(parseStatus(request.getStatus()));
        task.setPriority(parsePriority(request.getPriority()));
        task.setDeadline(request.getDeadline());
        task.setProject(project);
        task.setAssignedTo(assignee);
    }

    public TaskResponse createTask(TaskRequest request) {
        Task task = new Task();
        applyRequest(task, request);
        Task saved = taskRepository.save(task);
        notifyAssigned(saved);
        return toResponse(saved);
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = findTask(id);
        Long oldAssignee = task.getAssignedTo() != null ? task.getAssignedTo().getId() : null;
        Task.Status oldStatus = task.getStatus();
        applyRequest(task, request);
        Task saved = taskRepository.save(task);
        Long newAssignee = saved.getAssignedTo() != null ? saved.getAssignedTo().getId() : null;
        if (newAssignee != null && !newAssignee.equals(oldAssignee)) notifyAssigned(saved);
        if (oldStatus != saved.getStatus()) notifyStatusChange(saved);
        return toResponse(saved);
    }

    /** Employee: own tasks only. Admin: any task. Used by Kanban and "update status" buttons. */
    public TaskResponse updateStatus(Long id, String status) {
        Task task = findTask(id);
        AuthUser me = CurrentUser.get();
        if (!me.isAdmin() && (task.getAssignedTo() == null || !me.id().equals(task.getAssignedTo().getId()))) {
            throw new SecurityException("You can only update the status of your own tasks.");
        }
        Task.Status newStatus = parseStatus(status);
        if (newStatus == task.getStatus()) return toResponse(task);
        task.setStatus(newStatus);
        Task saved = taskRepository.save(task);
        notifyStatusChange(saved);
        return toResponse(saved);
    }

    public void deleteTask(Long id) {
        findTask(id);
        commentRepository.deleteByTaskId(id);
        taskRepository.deleteById(id);
    }

    // ---------- notifications ----------

    private void notifyAssigned(Task t) {
        if (t.getAssignedTo() == null || t.getAssignedTo().getId().equals(access.me().id())) return;
        notifications.notify(t.getAssignedTo(), "TASK_ASSIGNED",
                "You were assigned a new task: \"" + t.getTitle() + "\"", t.getId(), t.getProject().getId());
    }

    private void notifyStatusChange(Task t) {
        Long me = access.me().id();
        Long projectId = t.getProject() != null ? t.getProject().getId() : null;
        if (t.getAssignedTo() != null && !t.getAssignedTo().getId().equals(me)) {
            notifications.notify(t.getAssignedTo(), "STATUS_CHANGED",
                    "Task \"" + t.getTitle() + "\" moved to " + t.getStatus().name().replace('_', ' '),
                    t.getId(), projectId);
        }
        if (t.getStatus() == Task.Status.COMPLETED) {
            notifications.notifyAdmins(me, "TASK_COMPLETED",
                    "Task \"" + t.getTitle() + "\" was completed", t.getId(), projectId);
        }
    }
}
