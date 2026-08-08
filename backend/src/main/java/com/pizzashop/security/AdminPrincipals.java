package com.pizzashop.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** Reads the signed-in admin's email out of an authentication, whatever produced it. */
public final class AdminPrincipals {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private AdminPrincipals() {
    }

    public static String emailOf(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        return authentication.getName();
    }
}
