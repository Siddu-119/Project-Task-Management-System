package com.projectmgmt.dto;

import com.projectmgmt.logic.RecommendationEngine.Recommendation;
import java.util.List;

public record RecommendationResponse(String method, String disclaimer, List<Recommendation> recommendations) {

    public static final String DISCLAIMER =
            "Suggestions use fixed rules (skills, workload, overdue tasks and capacity). "
          + "They are not machine learning, and nothing is assigned until you confirm.";

    public static RecommendationResponse of(List<Recommendation> list) {
        return new RecommendationResponse("RULE_BASED", DISCLAIMER, list);
    }
}
