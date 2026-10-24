package com.projectmgmt.logic;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class WorkingDays {
    private WorkingDays() {}

    /** Hours of focused work we assume one person can do per working day (documented assumption). */
    public static final double HOURS_PER_DAY = 6.0;

    /** Monday-Friday days from 'from' to 'toInclusive', both included. 0 if 'toInclusive' is before 'from'. */
    public static int between(LocalDate from, LocalDate toInclusive) {
        if (from == null || toInclusive == null || toInclusive.isBefore(from)) return 0;
        int count = 0;
        LocalDate d = from;
        int guard = 0;
        while (!d.isAfter(toInclusive) && guard++ < 3660) {
            DayOfWeek w = d.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) count++;
            d = d.plusDays(1);
        }
        return count;
    }

    public static String hours(double h) {
        return h == Math.rint(h) ? String.valueOf((long) h) : String.format(java.util.Locale.ROOT, "%.1f", h);
    }
}
