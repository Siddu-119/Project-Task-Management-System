package com.projectmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ProjectRequest {
    @NotBlank(message = "Project name is required")
    @Size(max = 200, message = "Project name must be at most 200 characters")
    private String name;

    @Size(max = 5000, message = "Description is too long")
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;
    private String status;   // PLANNING, ACTIVE, ON_HOLD, COMPLETED (default PLANNING)
}
