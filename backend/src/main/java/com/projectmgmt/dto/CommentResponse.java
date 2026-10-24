package com.projectmgmt.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private Long id;
    private Long taskId;
    private Long userId;
    private String userName;
    private String text;
    private LocalDateTime createdAt;
}
