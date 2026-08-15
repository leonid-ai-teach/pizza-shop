package com.pizzashop.security;

import org.springframework.security.core.Authentication;

/** Reads the signed-in admin's email out of an authentication. */
public final class AdminPrincipals {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private AdminPrincipals() {
    }

    /**
     * The username <em>is</em> the email: {@link AdminUserDetailsService} builds the principal
     * from the primary key of {@code admin_access}.
     */
    public static String emailOf(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
