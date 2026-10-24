package com.projectmgmt.service;

import com.projectmgmt.logic.SkillNames;
import com.projectmgmt.model.Skill;
import com.projectmgmt.model.User;
import com.projectmgmt.repository.SkillRepository;
import com.projectmgmt.repository.UserRepository;
import com.projectmgmt.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SkillService {

    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private UserRepository userRepository;

    public static List<String> names(Collection<Skill> skills) {
        if (skills == null) return new ArrayList<>();
        return skills.stream().map(Skill::getName).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
    }

    public List<String> allNames() {
        return skillRepository.findAllByOrderByNameAsc().stream().map(Skill::getName).collect(Collectors.toList());
    }

    /** Cleans the names, then finds each skill or creates it (case-insensitive, so "java" reuses "Java"). */
    public Set<Skill> resolve(Collection<String> rawNames) {
        Set<Skill> result = new LinkedHashSet<>();
        for (String name : SkillNames.normalize(rawNames)) {
            result.add(findOrCreate(name));
        }
        return result;
    }

    private Skill findOrCreate(String name) {
        return skillRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            try {
                return skillRepository.save(new Skill(name));
            } catch (DataIntegrityViolationException e) {   // someone created it a moment ago
                return skillRepository.findByNameIgnoreCase(name).orElseThrow(() -> e);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<String> getUserSkills(Long userId) {
        var me = CurrentUser.get();
        if (!me.isAdmin() && !me.id().equals(userId)) {
            throw new SecurityException("You can only view your own skills.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return names(user.getSkills());
    }

    /** Admin only (also enforced in SecurityConfig). */
    @Transactional
    public List<String> setUserSkills(Long userId, List<String> rawNames) {
        if (!CurrentUser.get().isAdmin()) {
            throw new SecurityException("Only admins can change a person's skills.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setSkills(resolve(rawNames));
        userRepository.save(user);
        return names(user.getSkills());
    }
}
