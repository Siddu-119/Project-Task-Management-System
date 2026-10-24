package com.projectmgmt.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MemberResponse {
    private Long userId;
    private String name;
    private String email;
    private String role;
    private LocalDateTime addedAt;
    // workload inside this project
    private int totalTasks;
    private int openTasks;
    private int completedTasks;
    private int overdueTasks;
}
