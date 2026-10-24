package com.projectmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank(message = "Comment cannot be empty")
    @Size(max = 2000, message = "Comment must be at most 2000 characters")
    private String text;
}
