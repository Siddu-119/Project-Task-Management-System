package com.projectmgmt.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class SkillsRequest {
    @Size(max = 10, message = "At most 10 skills are allowed")
    private List<String> skills;
}
