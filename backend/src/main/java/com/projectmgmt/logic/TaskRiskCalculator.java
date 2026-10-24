package com.projectmgmt.logic;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * RULE-BASED risk that ONE task misses its due date. Not machine learning.
 * Points (cap 100): LOW below 30, MEDIUM 30-59, HIGH 60 and above.
 *
 *   Time pressure  up to 40   due today 40, within 2 days 30, within 7 days 15 - scaled by work left
 *                             (To Do 100%, In Progress 60%, Review 20%)
 *   Effort         up to 25   estimated hours left vs working days x 6h
 *   Assignee       up to 20   nobody assigned 15; otherwise load (5+ active tasks 10, 8+ 20) plus overdue (1 = 5, 2+ = 10)
 *   Priority       up to 15   URGENT 15, HIGH 8
 *
 * Completed = LOW; overdue = HIGH; no due date = UNKNOWN.
 */
public final class TaskRiskCalculator {
    private TaskRiskCalculator() {}

    public record Input(LocalDate today, String status, String priority, LocalDate deadline,
                        Double estimatedHours, boolean assigned, int assigneeActiveTasks, int assigneeOverdueTasks) {}

    public static RiskResult calculate(Input in) {
        List<String> reasons = new ArrayList<>(), positives = new ArrayList<>(), notes = new ArrayList<>();

        if ("COMPLETED".equals(in.status())) {
            positives.add("The task is completed.");
            return RiskResult.of(RiskLevel.LOW, 0, reasons, positives, notes);
        }
        if (in.deadline() == null) {
            notes.add("No due date is set, so delay risk cannot be estimated.");
            return RiskResult.of(RiskLevel.UNKNOWN, 0, reasons, positives, notes);
        }
        long daysLeft = ChronoUnit.DAYS.between(in.today(), in.deadline());
        if (daysLeft < 0) {
            reasons.add("The task is overdue by " + (-daysLeft) + " day(s) (+100).");
            return RiskResult.of(RiskLevel.HIGH, 100, reasons, positives, notes);
        }

        double fraction = switch (in.status() == null ? "TODO" : in.status()) {
            case "IN_PROGRESS" -> 0.6;
            case "REVIEW" -> 0.2;
            default -> 1.0;
        };
        int score = 0;

        // time pressure
        int base = daysLeft == 0 ? 40 : (daysLeft <= 2 ? 30 : (daysLeft <= 7 ? 15 : 0));
        int timePts = (int) Math.round(base * fraction);
        if (timePts > 0) {
            reasons.add((daysLeft == 0 ? "Due today" : "Due in " + daysLeft + " day(s)") + " and the status is "
                    + (in.status() == null ? "TODO" : in.status()).replace('_', ' ') + " (+" + timePts + ").");
            score += timePts;
        } else positives.add("The due date is " + daysLeft + " day(s) away.");

        // effort
        if (in.estimatedHours() != null) {
            double remaining = in.estimatedHours() * fraction;
            double available = WorkingDays.between(in.today(), in.deadline()) * WorkingDays.HOURS_PER_DAY;
            int effortPts = remaining > available ? 25 : (remaining > 0.7 * available ? 12 : 0);
            if (effortPts > 0) {
                reasons.add("About " + WorkingDays.hours(remaining) + "h of work is left but there are only "
                        + WorkingDays.hours(available) + "h of working time before the due date (+" + effortPts + ").");
                score += effortPts;
            } else positives.add("The remaining effort fits the time available.");
        } else {
            notes.add("No estimated hours, so effort was not scored.");
        }

        // assignee
        if (!in.assigned()) {
            reasons.add("Nobody is assigned (+15).");
            score += 15;
        } else {
            int load = in.assigneeActiveTasks() >= 8 ? 20 : (in.assigneeActiveTasks() >= 5 ? 10 : 0);
            int over = in.assigneeOverdueTasks() >= 2 ? 10 : (in.assigneeOverdueTasks() == 1 ? 5 : 0);
            int pts = Math.min(20, load + over);
            if (pts > 0) {
                reasons.add("The assignee has " + in.assigneeActiveTasks() + " active and " + in.assigneeOverdueTasks()
                        + " overdue task(s) (+" + pts + ").");
                score += pts;
            } else positives.add("The assignee's workload is manageable.");
        }

        // priority
        int prio = "URGENT".equals(in.priority()) ? 15 : ("HIGH".equals(in.priority()) ? 8 : 0);
        if (prio > 0) {
            reasons.add("Priority is " + in.priority() + " (+" + prio + ").");
            score += prio;
        }

        score = Math.min(100, score);
        return RiskResult.of(RiskLevel.fromScore(score), score, reasons, positives, notes);
    }
}
