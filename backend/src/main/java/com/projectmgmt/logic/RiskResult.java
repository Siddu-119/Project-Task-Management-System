package com.projectmgmt.logic;

import java.util.List;

/**
 * Result of a rule-based risk estimate.
 * reasons   = factors that INCREASE the risk, each with its points, e.g. "... (+14)"
 * positives = good signs
 * notes     = things that could not be scored (missing data) or assumptions used
 */
public record RiskResult(RiskLevel level, int score, List<String> reasons, List<String> positives,
                         List<String> notes, String method, String disclaimer) {

    public static final String METHOD = "RULE_BASED";
    public static final String DISCLAIMER =
            "Rule-based estimate from current data. It is not a machine-learning prediction.";

    public RiskResult withNote(String note) {
        List<String> all = new java.util.ArrayList<>(notes);
        all.add(note);
        return new RiskResult(level, score, reasons, positives, List.copyOf(all), method, disclaimer);
    }

    public static RiskResult of(RiskLevel level, int score, List<String> reasons, List<String> positives,
                                List<String> notes) {
        return new RiskResult(level, score, List.copyOf(reasons), List.copyOf(positives), List.copyOf(notes),
                METHOD, DISCLAIMER);
    }
}
