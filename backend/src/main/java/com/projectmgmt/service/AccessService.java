package com.projectmgmt.service;

import com.projectmgmt.model.ProjectMember;
import com.projectmgmt.repository.ProjectMemberRepository;
import com.projectmgmt.security.AuthUser;
import com.projectmgmt.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/** One place for "who is allowed to see what". Admin sees everything; employees see their projects. */
@Service
public class AccessService {

    @Autowired
    private ProjectMemberRepository memberRepository;

    public AuthUser me() {
        return CurrentUser.get();
    }

    public Set<Long> myProjectIds() {
        return memberRepository.findByUserId(me().id()).stream()
                .map(m -> m.getProject().getId()).collect(Collectors.toSet());
    }

    public boolean canAccessProject(Long projectId) {
        AuthUser u = me();
        return u.isAdmin() || memberRepository.existsByProjectIdAndUserId(projectId, u.id());
    }

    public void requireProjectAccess(Long projectId) {
        if (!canAccessProject(projectId)) {
            throw new SecurityException("You are not a member of this project.");
        }
    }
}
