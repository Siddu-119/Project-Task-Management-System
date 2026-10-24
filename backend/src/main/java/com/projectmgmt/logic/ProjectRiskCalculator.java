package com.projectmgmt.logic;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * RULE-BASED project delay risk. Not machine learning - there is no historical data to train on,
 * and none is invented. Points (cap 100): LOW below 30, MEDIUM 30-59, HIGH 60 and above.
 *
 *   Overdue tasks        up to 35   35 x (overdue open tasks / open tasks)
 *   Behind schedule      up to 35   gap between time used and tasks done: 70 points per 100% gap
 *   Pace needed          up to 20   tasks/day needed vs tasks/day finished in the last 14 days
 *   Effort vs capacity   up to 10   remaining estimated hours vs working days x 6h x assignees
 *   Unassigned tasks     up to 10   10 x (unassigned open tasks / open tasks)
 *
 * Special cases: Completed project or all tasks done = LOW; end date passed with open tasks = HIGH;
 * no tasks or no end date = UNKNOWN (not enough data).
 */
public final class ProjectRiskCalculator {
    private ProjectRiskCalculator() {}

    public record Input(LocalDate today, LocalDate startDate, LocalDate endDate, String status,
                        int totalTasks, int completedTasks, int overdueOpenTasks, int unassignedOpenTasks,
                        int completedLast14Days, double remainingEstimatedHours,
                        int openTasksWithoutEstimate, int distinctAssignees) {}

    public static RiskResult calculate(Input in) {
        List<String> reasons = new ArrayList<>(), positives = new ArrayList<>(), notes = new ArrayList<>();
        int open = in.totalTasks() - in.completedTasks();

        if ("COMPLETED".equals(in.status())) {
            positives.add("The project is marked Completed.");
            return RiskResult.of(RiskLevel.LOW, 0, reasons, positives, notes);
        }
        if (in.totalTasks() == 0) {
            notes.add("This project has no tasks yet, so delay risk cannot be estimated.");
            return RiskResult.of(RiskLevel.UNKNOWN, 0, reasons, positives, notes);
        }
        if (open <= 0) {
            positives.add("All " + in.totalTasks() + " tasks are completed.");
            return RiskResult.of(RiskLevel.LOW, 0, reasons, positives, notes);
        }
        if (in.endDate() == null) {
            notes.add("No project end date is set, so deadline risk cannot be estimated.");
            return RiskResult.of(RiskLevel.UNKNOWN, 0, reasons, positives, notes);
        }

        long daysLeft = ChronoUnit.DAYS.between(in.today(), in.endDate());
        if (daysLeft < 0) {
            reasons.add("The end date passed " + (-daysLeft) + " day(s) ago and " + open + " task(s) are still open (+100).");
            return RiskResult.of(RiskLevel.HIGH, 100, reasons, positives, notes);
        }
        if ("ON_HOLD".equals(in.status())) notes.add("The project is On Hold, so its dates may be out of date.");

        int score = 0;

        // 1. overdue
        int overduePts = (int) Math.round(35.0 * in.overdueOpenTasks() / open);
        if (overduePts > 0) {
            reasons.add(in.overdueOpenTasks() + " of " + open + " open tasks are overdue (+" + overduePts + ").");
            score += overduePts;
        } else positives.add("No open task is overdue.");

        // 2. behind schedule
        if (in.startDate() != null && in.endDate().isAfter(in.startDate())) {
            long totalDays = ChronoUnit.DAYS.between(in.startDate(), in.endDate());
            long elapsed = Math.max(0, Math.min(totalDays, ChronoUnit.DAYS.between(in.startDate(), in.today())));
            double expected = (double) elapsed / totalDays;
            double actual = (double) in.completedTasks() / in.totalTasks();
            double gap = expected - actual;
            int gapPts = gap > 0 ? (int) Math.min(35, Math.round(gap * 70)) : 0;
            if (gapPts > 0) {
                reasons.add(pct(expected) + "% of the time has passed but only " + pct(actual) + "% of tasks are done (+" + gapPts + ").");
                score += gapPts;
            } else positives.add("Progress (" + pct(actual) + "% of tasks done) is on or ahead of schedule (" + pct(expected) + "% of time used).");
        } else {
            notes.add("No valid start date, so progress was not compared with the schedule.");
        }

        // 3. pace
        int pacePts;
        if (in.completedLast14Days() > 0) {
            double needed = (double) open / Math.max(1, daysLeft);
            double velocity = in.completedLast14Days() / 14.0;
            double ratio = needed / velocity;
            pacePts = ratio > 1.5 ? 20 : (ratio > 1.0 ? 10 : 0);
            if (pacePts > 0) reasons.add("The remaining " + open + " task(s) need about " + String.format(java.util.Locale.ROOT, "%.1f", ratio)
                    + "x the pace of the last 14 days (" + in.completedLast14Days() + " task(s) finished) (+" + pacePts + ").");
            else positives.add("The recent pace (" + in.completedLast14Days() + " task(s) finished in 14 days) is enough for the remaining work.");
        } else {
            pacePts = daysLeft <= 14 ? 20 : 10;
            reasons.add("No task was completed in the last 14 days and " + daysLeft + " day(s) remain (+" + pacePts + ").");
        }
        score += pacePts;

        // 4. effort vs capacity
        if (in.openTasksWithoutEstimate() > 0) {
            notes.add(in.openTasksWithoutEstimate() + " open task(s) have no estimated hours, so effort was not scored.");
        } else if (in.remainingEstimatedHours() > 0) {
            double capacity = WorkingDays.between(in.today(), in.endDate()) * WorkingDays.HOURS_PER_DAY * Math.max(1, in.distinctAssignees());
            int effortPts = in.remainingEstimatedHours() > capacity ? 10 : (in.remainingEstimatedHours() > 0.7 * capacity ? 5 : 0);
            if (effortPts > 0) {
                reasons.add(WorkingDays.hours(in.remainingEstimatedHours()) + "h of work remain but the team has about "
                        + WorkingDays.hours(capacity) + "h of working time before the end date (+" + effortPts + ").");
                score += effortPts;
            } else positives.add("Remaining effort (" + WorkingDays.hours(in.remainingEstimatedHours()) + "h) fits the available working time.");
            notes.add("Capacity assumes 6 productive hours per working day per assigned person.");
        }

        // 5. unassigned
        int unassignedPts = (int) Math.round(10.0 * in.unassignedOpenTasks() / open);
        if (unassignedPts > 0) {
            reasons.add(in.unassignedOpenTasks() + " open task(s) have nobody assigned (+" + unassignedPts + ").");
            score += unassignedPts;
        }

        score = Math.min(100, score);
        return RiskResult.of(RiskLevel.fromScore(score), score, reasons, positives, notes);
    }

    private static long pct(double fraction) {
        return Math.round(fraction * 100);
    }
}
