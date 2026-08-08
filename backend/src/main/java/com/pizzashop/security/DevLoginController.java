package com.pizzashop.security;

import com.pizzashop.service.AdminAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Signs in as the bootstrap admin without Google, so the admin area can be exercised locally
 * without Google Cloud credentials.
 *
 * <p>This bean only exists under the {@code localdev} profile and must never be enabled in a
 * deployed environment — it grants admin access to anyone who can reach the URL. It deliberately
 * produces the same {@link org.springframework.security.oauth2.core.oidc.user.OidcUser} principal
 * that a real Google login produces, so no downstream code has a separate dev path.
 */
@RestController
@Profile("localdev")
public class DevLoginController {

    private static final Logger log = LoggerFactory.getLogger(DevLoginController.class);

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    private final AdminAccessService adminAccessService;
    private final String bootstrapEmail;
    private final String frontendUrl;

    public DevLoginController(AdminAccessService adminAccessService,
                              @Value("${pizzashop.admin.bootstrap-email:}") String bootstrapEmail,
                              @Value("${pizzashop.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.adminAccessService = adminAccessService;
        this.bootstrapEmail = bootstrapEmail;
        this.frontendUrl = frontendUrl;
        log.warn("DEV LOGIN ENABLED at /dev-login - localdev profile only, never deploy this.");
    }

    @GetMapping("/dev-login")
    public void devLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!adminAccessService.isAllowed(bootstrapEmail)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "No bootstrap admin configured; set ADMIN_BOOTSTRAP_EMAIL.");
            return;
        }

        String email = AdminAccessService.normalize(bootstrapEmail);
        Instant now = Instant.now();
        OidcIdToken idToken = new OidcIdToken(
                "dev-token",
                now,
                now.plus(1, ChronoUnit.HOURS),
                Map.of("sub", email, "email", email, "name", "Local Dev Admin"));
        DefaultOidcUser principal = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority(AdminPrincipals.ROLE_ADMIN)), idToken, "email");

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        response.sendRedirect(frontendUrl + "/admin");
    }
}
