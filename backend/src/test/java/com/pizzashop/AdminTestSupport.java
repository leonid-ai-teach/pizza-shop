package com.pizzashop;

import com.pizzashop.security.AdminPrincipals;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

/** Shared helpers for simulating signed-in (and merely-authenticated) admins in MockMvc tests. */
public final class AdminTestSupport {

    private AdminTestSupport() {
    }

    /**
     * A session that already passed the allowlist check at login time, i.e. what
     * {@code AllowlistOidcUserService} produces for an approved Google account.
     */
    public static RequestPostProcessor allowlistedAdmin(String email) {
        return SecurityMockMvcRequestPostProcessors.oidcLogin()
                .authorities(new SimpleGrantedAuthority(AdminPrincipals.ROLE_ADMIN))
                .idToken(token -> token.claims(claims -> claims.putAll(
                        Map.of("sub", email, "email", email, "name", "Test Admin"))));
    }

    /**
     * A Google account that authenticated but was never approved: it has no admin authority,
     * standing in for anyone who signs in without being on the allowlist.
     */
    public static RequestPostProcessor nonAllowlistedUser(String email) {
        return SecurityMockMvcRequestPostProcessors.oidcLogin()
                .authorities(List.of())
                .idToken(token -> token.claims(claims -> claims.putAll(
                        Map.of("sub", email, "email", email, "name", "Outsider"))));
    }
}
