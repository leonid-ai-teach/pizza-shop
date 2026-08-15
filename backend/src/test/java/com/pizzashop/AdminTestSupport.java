package com.pizzashop;

import com.pizzashop.security.AdminPrincipals;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

/** Shared helpers for simulating signed-in (and merely-authenticated) admins in MockMvc tests. */
public final class AdminTestSupport {

    private AdminTestSupport() {
    }

    /**
     * A session for an admin who exists in {@code admin_access}, i.e. what
     * {@code AdminLoginController} produces after a successful login.
     */
    public static RequestPostProcessor allowlistedAdmin(String email) {
        return SecurityMockMvcRequestPostProcessors.user(email)
                .authorities(new SimpleGrantedAuthority(AdminPrincipals.ROLE_ADMIN));
    }

    /**
     * Someone authenticated but without admin authority. There is no way to reach this state
     * through the login endpoint any more — {@code AdminUserDetailsService} refuses unknown
     * emails outright — but the filter chain must still turn such a request away, so the
     * authorisation rule stays covered.
     */
    public static RequestPostProcessor nonAllowlistedUser(String email) {
        return SecurityMockMvcRequestPostProcessors.user(email).authorities(List.of());
    }
}
