package com.projectmgmt.repository;

import com.projectmgmt.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFalse(Long userId);
    boolean existsByUserIdAndTypeAndTaskId(Long userId, String type, Long taskId);
}
