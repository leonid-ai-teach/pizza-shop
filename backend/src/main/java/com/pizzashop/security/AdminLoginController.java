package com.pizzashop.security;

import com.pizzashop.dto.LoginRequest;
import com.pizzashop.exception.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Signs an admin in with email and password and puts the result into the HTTP session.
 *
 * <p>Deliberately a JSON endpoint instead of Spring Security's form-login filter: the SPA needs a
 * status code it can react to, not an HTML redirect. Everything downstream of the session — the
 * CSRF cookie, the 401 entry point, the logout endpoint — stays exactly as the filter chain
 * configures it.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminLoginController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AdminLoginController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void login(@Valid @RequestBody LoginRequest request,
                      HttpServletRequest httpRequest,
                      HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email(), request.password()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException("Email or password is not correct.");
        }

        // Authenticating by hand bypasses Spring Security's SessionAuthenticationStrategy, so the
        // session-fixation defence has to happen here: if the caller arrived with a session id
        // (which an attacker could have planted), it must not survive the privilege change.
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }
}
