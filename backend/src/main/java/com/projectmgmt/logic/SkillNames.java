package com.projectmgmt.logic;

import java.util.*;
import java.util.regex.Pattern;

/** Cleans up skill names typed by users: trims, removes duplicates (ignoring case), rejects odd characters. */
public final class SkillNames {
    public static final int MAX_SKILLS = 10;
    public static final int MAX_LENGTH = 40;
    // letters/digits/dot first, then letters, digits, space and + # . - /   (C++, C#, .NET, CI/CD, Node.js)
    private static final Pattern ALLOWED = Pattern.compile("^[\\p{L}\\p{N}.][\\p{L}\\p{N} +#.\\-/]*$");

    private SkillNames() {}

    public static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static List<String> normalize(Collection<String> raw) {
        if (raw == null) return List.of();
        Map<String, String> unique = new LinkedHashMap<>();
        for (String s : raw) {
            if (s == null) continue;
            String name = s.trim().replaceAll("\\s+", " ");
            if (name.isEmpty()) continue;
            if (name.length() > MAX_LENGTH) {
                throw new IllegalArgumentException("Each skill name must be at most " + MAX_LENGTH + " characters.");
            }
            if (!ALLOWED.matcher(name).matches()) {
                throw new IllegalArgumentException("Skill names may only contain letters, numbers, spaces and + # . - /");
            }
            unique.putIfAbsent(key(name), name);
        }
        if (unique.size() > MAX_SKILLS) {
            throw new IllegalArgumentException("At most " + MAX_SKILLS + " skills are allowed.");
        }
        return new ArrayList<>(unique.values());
    }
}
