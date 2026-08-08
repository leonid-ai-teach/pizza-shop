package com.pizzashop.security;

import com.pizzashop.service.AdminAccessService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rejects any Google account whose email is not on the {@code AdminAccess} allowlist. Access is
 * by prior invitation only, so an unknown account is turned away at login rather than being
 * created in a pending state (docs/adr/0001-google-oauth-admin-auth.md).
 */
@Component
public class AllowlistOidcUserService extends OidcUserService {

    static final String NOT_ALLOWLISTED_ERROR = "not_allowlisted";

    private final AdminAccessService adminAccessService;

    public AllowlistOidcUserService(AdminAccessService adminAccessService) {
        this.adminAccessService = adminAccessService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser user = super.loadUser(userRequest);
        String email = user.getEmail();

        if (!adminAccessService.isAllowed(email)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(NOT_ALLOWLISTED_ERROR,
                            "This Google account is not approved for the admin area.", null));
        }

        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority(AdminPrincipals.ROLE_ADMIN)),
                user.getIdToken(),
                user.getUserInfo(),
                "email");
    }
}
