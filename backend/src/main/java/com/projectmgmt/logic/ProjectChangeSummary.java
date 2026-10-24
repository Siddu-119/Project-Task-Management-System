package com.projectmgmt.logic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Describes what changed on a project, so team members are only notified about real changes. */
public final class ProjectChangeSummary {
    private ProjectChangeSummary() {}

    private static String pretty(String status) {
        return status == null ? "not set" : status.replace('_', ' ');
    }

    private static String date(LocalDate d) {
        return d == null ? "not set" : d.toString();
    }

    public static List<String> describe(String oldName, String newName, String oldStatus, String newStatus,
                                        LocalDate oldStart, LocalDate newStart, LocalDate oldEnd, LocalDate newEnd) {
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(oldName, newName)) changes.add("name changed from \"" + oldName + "\" to \"" + newName + "\"");
        if (!Objects.equals(oldStatus, newStatus)) changes.add("status changed from " + pretty(oldStatus) + " to " + pretty(newStatus));
        if (!Objects.equals(oldStart, newStart)) changes.add("start date changed from " + date(oldStart) + " to " + date(newStart));
        if (!Objects.equals(oldEnd, newEnd)) changes.add("end date changed from " + date(oldEnd) + " to " + date(newEnd));
        return changes;
    }

    public static String message(String projectName, List<String> changes) {
        return "Project \"" + projectName + "\" was updated: " + String.join("; ", changes);
    }
}
