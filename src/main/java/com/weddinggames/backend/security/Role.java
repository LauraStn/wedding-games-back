package com.weddinggames.backend.security;

/**
 * Application-wide roles. ADMIN, INTERVENANT, JURY and PROJECTION are held by
 * {@code StaffAccount}s with a real password; PARTICIPANT is granted to a
 * guest session created after a successful invitation confirmation.
 */
public enum Role {
    ADMIN,
    INTERVENANT,
    JURY,
    PARTICIPANT,
    PROJECTION;

    public String authority() {
        return "ROLE_" + name();
    }
}
