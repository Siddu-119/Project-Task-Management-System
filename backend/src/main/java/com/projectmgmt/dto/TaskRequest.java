package com.projectmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class TaskRequest {
    @NotBlank(message = "Task title is required")
    @Size(max = 200, message = "Task title must be at most 200 characters")
    private String title;

    @Size(max = 5000, message = "Description is too long")
    private String description;

    private String status;      // TODO, IN_PROGRESS, REVIEW, COMPLETED (default TODO)
    private String priority;    // LOW, MEDIUM, HIGH, URGENT (default MEDIUM)
    private LocalDate deadline; // shown as "Due date" in the UI

    @NotNull(message = "Please choose a project")
    private Long projectId;

    private Long assignedToId;  // null = unassigned
}
