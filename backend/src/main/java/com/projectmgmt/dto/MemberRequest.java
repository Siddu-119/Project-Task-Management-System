package com.projectmgmt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MemberRequest {
    @NotNull(message = "Please choose a user")
    private Long userId;
}
