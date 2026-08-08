package com.pizzashop.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final AllowlistOidcUserService allowlistOidcUserService;
    private final String frontendUrl;

    public SecurityConfig(AllowlistOidcUserService allowlistOidcUserService,
                          @Value("${pizzashop.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.allowlistOidcUserService = allowlistOidcUserService;
        this.frontendUrl = frontendUrl;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // The admin API is authenticated by session cookie, so it needs CSRF protection.
                // spa() pairs a JS-readable XSRF-TOKEN cookie with the X-XSRF-TOKEN header, which
                // is exactly what Angular's HttpClient sends by default. The public customer
                // endpoints are exempt: they carry no ambient authority for an attacker to abuse.
                .csrf(csrf -> csrf
                        .spa()
                        .ignoringRequestMatchers("/api/pizzas/**", "/api/toppings/**", "/api/orders/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/pizzas", "/api/pizzas/*", "/api/toppings")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/*").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**", "/error").permitAll()
                        // DevLoginController only exists under the localdev profile, so this
                        // path simply 404s everywhere else.
                        .requestMatchers(HttpMethod.GET, "/dev-login").permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority(AdminPrincipals.ROLE_ADMIN)
                        .anyRequest().authenticated())
                // XHR calls must get a 401 they can react to, not an HTML redirect to Google.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/admin/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .deleteCookies("JSESSIONID"));

        // Google login is only wired up when credentials are actually configured, so the app
        // still boots for tests and for local dev without Google Cloud credentials.
        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(allowlistOidcUserService))
                    .defaultSuccessUrl(frontendUrl + "/admin", true)
                    .failureUrl(frontendUrl + "/admin/login?error"));
        } else {
            log.warn("No Google OAuth2 client registration found; admin login via Google is "
                    + "disabled. Set SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID "
                    + "and _CLIENT_SECRET to enable it (see .env.example).");
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // Session cookie and XSRF token must travel with admin requests.
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
