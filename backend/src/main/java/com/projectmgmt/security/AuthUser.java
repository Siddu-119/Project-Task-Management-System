package com.projectmgmt.security;

/** The logged-in user, rebuilt from the JWT on every request. */
public record AuthUser(Long id, String email, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
