package com.projectmgmt.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String type;
    private String message;
    private boolean read;
    private Long taskId;
    private Long projectId;
    private LocalDateTime createdAt;
}
