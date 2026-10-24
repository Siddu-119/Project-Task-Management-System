package com.projectmgmt.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Helper for services that need to know who is making the request. */
public final class CurrentUser {
    private CurrentUser() {}

    public static AuthUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser)) {
            throw new IllegalStateException("No authenticated user");
        }
        return (AuthUser) auth.getPrincipal();
    }
}
