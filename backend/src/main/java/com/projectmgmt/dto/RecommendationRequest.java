package com.projectmgmt.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/** Describes the task you are about to assign. Nothing is saved or assigned by asking for a recommendation. */
@Data
public class RecommendationRequest {
    @NotNull(message = "Please choose a project")
    private Long projectId;

    @Size(max = 10, message = "At most 10 skills are allowed")
    private List<String> skills;

    private String priority;      // LOW, MEDIUM, HIGH, URGENT (default MEDIUM)
    private LocalDate deadline;

    @DecimalMin(value = "0.1", message = "Estimated hours must be between 0.1 and 999")
    @DecimalMax(value = "999", message = "Estimated hours must be between 0.1 and 999")
    private Double estimatedHours;

    private Long taskId;          // when editing an existing task: its own hours are not counted against its assignee
}
