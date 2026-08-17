package com.weddinggames.backend.staff;

import com.weddinggames.backend.security.Role;

public enum StaffRole {
    ADMIN(Role.ADMIN),
    INTERVENANT(Role.INTERVENANT),
    JURY(Role.JURY),
    PROJECTION(Role.PROJECTION);

    private final Role securityRole;

    StaffRole(Role securityRole) {
        this.securityRole = securityRole;
    }

    public Role toSecurityRole() {
        return securityRole;
    }
}
