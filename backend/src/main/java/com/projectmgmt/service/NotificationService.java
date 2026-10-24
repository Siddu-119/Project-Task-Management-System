package com.projectmgmt.service;

import com.projectmgmt.dto.NotificationResponse;
import com.projectmgmt.model.Notification;
import com.projectmgmt.model.Task;
import com.projectmgmt.model.User;
import com.projectmgmt.repository.NotificationRepository;
import com.projectmgmt.repository.TaskRepository;
import com.projectmgmt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    public void notify(User user, String type, String message, Long taskId, Long projectId) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setMessage(message.length() > 500 ? message.substring(0, 500) : message);
        n.setTaskId(taskId);
        n.setProjectId(projectId);
        notificationRepository.save(n);
    }

    /** Tell every admin (except the person who did the action) - used for "task completed". */
    public void notifyAdmins(Long exceptUserId, String type, String message, Long taskId, Long projectId) {
        for (User admin : userRepository.findByRole(User.Role.ADMIN)) {
            if (!admin.getId().equals(exceptUserId)) {
                notify(admin, type, message, taskId, projectId);
            }
        }
    }

    /** Creates a DUE_SOON notification (once per task) for open tasks due in the next 2 days. */
    private void generateDueSoon(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(2);
        for (Task t : taskRepository.findByAssignedToId(userId)) {
            if (t.getStatus() != Task.Status.COMPLETED && t.getDeadline() != null
                    && !t.getDeadline().isBefore(today) && !t.getDeadline().isAfter(limit)
                    && !notificationRepository.existsByUserIdAndTypeAndTaskId(userId, "DUE_SOON", t.getId())) {
                notify(t.getAssignedTo(), "DUE_SOON",
                        "Task \"" + t.getTitle() + "\" is due on " + t.getDeadline(),
                        t.getId(), t.getProject() != null ? t.getProject().getId() : null);
            }
        }
    }

    public List<NotificationResponse> list(Long userId) {
        generateDueSoon(userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public long unreadCount(Long userId) {
        generateDueSoon(userId);
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public void markRead(Long id, Long userId) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!n.getUser().getId().equals(userId)) {
            throw new SecurityException("This notification is not yours.");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    public void markAllRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> !n.isRead()).collect(Collectors.toList());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setType(n.getType());
        r.setMessage(n.getMessage());
        r.setRead(n.isRead());
        r.setTaskId(n.getTaskId());
        r.setProjectId(n.getProjectId());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }
}
