package com.projectmgmt.controller;

import com.projectmgmt.dto.RecommendationRequest;
import com.projectmgmt.dto.RecommendationResponse;
import com.projectmgmt.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    /** Admin only (SecurityConfig). Returns a ranked list with reasons. Does NOT assign or save anything. */
    @PostMapping("/api/recommendations/assignee")
    public RecommendationResponse recommendAssignee(@Valid @RequestBody RecommendationRequest request) {
        return recommendationService.recommend(request);
    }
}
