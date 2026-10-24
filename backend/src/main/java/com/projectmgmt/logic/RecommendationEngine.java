package com.projectmgmt.logic;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Explainable, RULE-BASED assignee recommendation. No machine learning, no randomness.
 *
 * Every candidate gets points (maximum 100) from four fixed rules:
 *   Skills    40  share of the task's required skills the person has (no required skills = neutral 20)
 *   Workload  30  30 x (1 - activeTasks / 8), never below 0
 *   Overdue   15  15 - 5 per overdue task (10 per overdue task when the task is HIGH/URGENT), never below 0
 *   Capacity  15  can the estimated hours fit before the deadline? (no estimate/deadline = neutral 7)
 *                 free hours = working days x 6 - hours already open; fits = 15, at least half = 8, else 0
 *
 * The engine only RANKS people. It never assigns anything - a person must confirm.
 */
public final class RecommendationEngine {
    public static final int W_SKILLS = 40, W_WORKLOAD = 30, W_OVERDUE = 15, W_CAPACITY = 15;
    public static final int MAX_ACTIVE_TASKS = 8;
    public static final double DEFAULT_TASK_HOURS = 4.0;   // used for open tasks that have no estimate

    private RecommendationEngine() {}

    public record Candidate(Long userId, String name, Collection<String> skills, int activeTasks,
                            int overdueTasks, double openHours, boolean currentAssignee) {}

    public record TaskNeed(Collection<String> requiredSkills, String priority, LocalDate deadline,
                           Double estimatedHours, LocalDate today) {}

    public record Factor(String name, int points, int max, String detail) {}

    public record Recommendation(Long userId, String name, int score, boolean best, boolean currentAssignee,
                                 List<Factor> factors, String summary) {}

    private record Scored(Candidate candidate, int score, List<Factor> factors) {}

    public static List<Recommendation> recommend(TaskNeed need, List<Candidate> candidates) {
        List<Scored> scored = new ArrayList<>();
        for (Candidate c : candidates) scored.add(score(need, c));
        scored.sort(Comparator.<Scored>comparingInt(s -> -s.score)
                .thenComparingInt(s -> s.candidate.activeTasks())
                .thenComparing(s -> s.candidate.name(), String.CASE_INSENSITIVE_ORDER));

        List<Recommendation> result = new ArrayList<>();
        for (int i = 0; i < scored.size(); i++) {
            Scored s = scored.get(i);
            boolean best = i == 0;
            result.add(new Recommendation(s.candidate.userId(), s.candidate.name(), s.score, best,
                    s.candidate.currentAssignee(), s.factors, summary(s, best)));
        }
        return result;
    }

    private static Scored score(TaskNeed need, Candidate c) {
        List<Factor> factors = new ArrayList<>();

        // ---- skills ----
        List<String> required = distinct(need.requiredSkills());
        if (required.isEmpty()) {
            factors.add(new Factor("Skills", W_SKILLS / 2, W_SKILLS, "Task has no required skills, so skills were not compared"));
        } else {
            Set<String> have = new HashSet<>();
            if (c.skills() != null) for (String s : c.skills()) have.add(SkillNames.key(s));
            List<String> matched = new ArrayList<>(), missing = new ArrayList<>();
            for (String r : required) (have.contains(SkillNames.key(r)) ? matched : missing).add(r);
            int pts = (int) Math.round((double) W_SKILLS * matched.size() / required.size());
            String detail = missing.isEmpty()
                    ? "Has all " + required.size() + " required skill(s): " + String.join(", ", matched)
                    : (matched.isEmpty() ? "Has none of the required skills." : "Has " + matched.size() + " of " + required.size()
                        + " required skills (" + String.join(", ", matched) + ").")
                      + " Missing skills: " + String.join(", ", missing);
            factors.add(new Factor("Skills", pts, W_SKILLS, detail));
        }

        // ---- workload ----
        int wl = (int) Math.round(W_WORKLOAD * Math.max(0.0, 1.0 - (double) c.activeTasks() / MAX_ACTIVE_TASKS));
        String active = c.activeTasks() == 0 ? "No active tasks" : c.activeTasks() + " active task" + (c.activeTasks() == 1 ? "" : "s");
        factors.add(new Factor("Workload", wl, W_WORKLOAD, active));

        // ---- overdue ----
        boolean urgent = "HIGH".equals(need.priority()) || "URGENT".equals(need.priority());
        int perTask = urgent ? 10 : 5;
        int od = Math.max(0, W_OVERDUE - perTask * c.overdueTasks());
        String odText = c.overdueTasks() == 0 ? "No overdue tasks"
                : c.overdueTasks() + " overdue task" + (c.overdueTasks() == 1 ? "" : "s")
                  + (urgent ? " (counted double because this task is high priority)" : "");
        factors.add(new Factor("Overdue", od, W_OVERDUE, odText));

        // ---- capacity ----
        factors.add(capacity(need, c));

        int total = factors.stream().mapToInt(Factor::points).sum();
        return new Scored(c, total, factors);
    }

    private static Factor capacity(TaskNeed need, Candidate c) {
        if (need.estimatedHours() == null || need.deadline() == null || need.today() == null) {
            return new Factor("Capacity", W_CAPACITY / 2, W_CAPACITY, "No estimate or due date, so free time was not compared");
        }
        double est = need.estimatedHours();
        if (need.deadline().isBefore(need.today())) {
            return new Factor("Capacity", 0, W_CAPACITY, "The due date has already passed");
        }
        double free = WorkingDays.between(need.today(), need.deadline()) * WorkingDays.HOURS_PER_DAY - c.openHours();
        String freeText = "about " + WorkingDays.hours(Math.max(0, free)) + "h free before the due date";
        if (free >= est) return new Factor("Capacity", W_CAPACITY, W_CAPACITY, WorkingDays.hours(est) + "h of work fits: " + freeText);
        if (free >= est / 2) return new Factor("Capacity", 8, W_CAPACITY, "Tight: " + WorkingDays.hours(est) + "h of work but only " + freeText);
        return new Factor("Capacity", 0, W_CAPACITY, "Probably cannot fit " + WorkingDays.hours(est) + "h of work: only " + freeText);
    }

    // ---- plain-language summary ----

    private static String summary(Scored s, boolean best) {
        List<String> good = new ArrayList<>(), bad = new ArrayList<>();
        for (Factor f : s.factors) {
            switch (f.name()) {
                case "Skills" -> {
                    if (f.detail().startsWith("Has all")) good.add("has all the required skills");
                    else if (f.detail().contains("Missing skills")) bad.add("is missing some required skills");
                }
                case "Workload" -> {
                    if (f.points() >= 22) good.add("has fewer active assignments");
                    else if (f.points() <= 11) bad.add("already has many active assignments");
                }
                case "Overdue" -> {
                    if (f.points() == W_OVERDUE) good.add("has no overdue tasks");
                    else bad.add("has overdue tasks");
                }
                case "Capacity" -> {
                    if (f.points() == W_CAPACITY && f.detail().contains("fits")) good.add("has enough free time before the due date");
                    else if (f.points() == 0 && !f.detail().startsWith("No estimate")) bad.add("may not have enough time before the due date");
                }
                default -> { }
            }
        }
        String head;
        if (best) head = good.isEmpty() ? "Highest score in this team, but no single factor stands out."
                : "Recommended because this member " + join(good) + ".";
        else head = good.isEmpty() ? "Lower fit for this task." : "Good on: " + join(good) + ".";
        return bad.isEmpty() ? head : head + " Concerns: this member " + join(bad) + ".";
    }

    private static String join(List<String> parts) {
        if (parts.size() == 1) return parts.get(0);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
    }

    private static List<String> distinct(Collection<String> raw) {
        if (raw == null) return List.of();
        Map<String, String> m = new LinkedHashMap<>();
        for (String s : raw) if (s != null && !s.isBlank()) m.putIfAbsent(SkillNames.key(s.trim()), s.trim());
        return new ArrayList<>(m.values());
    }

    static String names(List<Recommendation> list) {
        return list.stream().map(Recommendation::name).collect(Collectors.joining(","));
    }
}
