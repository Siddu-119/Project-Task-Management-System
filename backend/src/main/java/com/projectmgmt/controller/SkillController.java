package com.projectmgmt.controller;

import com.projectmgmt.dto.SkillsRequest;
import com.projectmgmt.security.CurrentUser;
import com.projectmgmt.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SkillController {

    @Autowired
    private SkillService skillService;

    /** All skill names (for suggestions in forms). Skill names are not personal data. */
    @GetMapping("/api/skills")
    public List<String> allSkills() {
        return skillService.allNames();
    }

    @GetMapping("/api/users/me/skills")
    public List<String> mySkills() {
        return skillService.getUserSkills(CurrentUser.get().id());
    }

    /** Admin only (SecurityConfig). Replaces the person's skill list. */
    @PutMapping("/api/users/{id}/skills")
    public List<String> setSkills(@PathVariable Long id, @Valid @RequestBody SkillsRequest request) {
        return skillService.setUserSkills(id, request.getSkills());
    }
}
