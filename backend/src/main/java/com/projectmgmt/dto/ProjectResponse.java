package com.projectmgmt.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createdAt;
    // calculated from tasks - never stored
    private int progress;
    private int totalTasks;
    private int todoTasks;
    private int inProgressTasks;
    private int reviewTasks;
    private int completedTasks;
    private int overdueTasks;
    private long memberCount;
}
