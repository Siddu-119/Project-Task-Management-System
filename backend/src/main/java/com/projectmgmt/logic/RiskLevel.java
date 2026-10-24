package com.projectmgmt.logic;

public enum RiskLevel {
    LOW, MEDIUM, HIGH,
    /** Not enough data to say anything honest (for example: no tasks, or no end date). */
    UNKNOWN;

    public static RiskLevel fromScore(int score) {
        if (score >= 60) return HIGH;
        if (score >= 30) return MEDIUM;
        return LOW;
    }
}
