package com.pizzashop.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String frontendUrl;

    public SecurityConfig(@Value("${pizzashop.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // The admin API is authenticated by session cookie, so it needs CSRF protection.
                // spa() pairs a JS-readable XSRF-TOKEN cookie with the X-XSRF-TOKEN header, which
                // is exactly what Angular's HttpClient sends by default. The public customer
                // endpoints are exempt: they carry no ambient authority for an attacker to abuse.
                //
                // The login endpoint is exempt for the same reason — it runs before a session
                // exists, so there is no authority to ride on. Requiring a token there would mean
                // fetching one with an extra request just to be able to sign in.
                .csrf(csrf -> csrf
                        .spa()
                        .ignoringRequestMatchers("/api/pizzas/**", "/api/toppings/**", "/api/orders/**",
                                "/api/admin/login"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/pizzas", "/api/pizzas/*", "/api/toppings")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/login").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority(AdminPrincipals.ROLE_ADMIN)
                        .anyRequest().authenticated())
                // XHR calls must get a 401 they can react to, not an HTML redirect to a login page.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/admin/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .deleteCookies("JSESSIONID"));

        return http.build();
    }

    /**
     * Published as a bean because {@link AdminLoginController} authenticates by hand rather than
     * through a form-login filter; the SPA needs a JSON endpoint, not an HTML redirect.
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * The delegating encoder stores the algorithm alongside the hash ("{bcrypt}$2a$10$…"), so a
     * later move to a different one can rehash gradually instead of invalidating every password
     * at once.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Only relevant when the SPA is served from a different origin than the API, i.e. the Angular
     * dev server on :4200. In the Docker stack nginx puts both behind one origin and none of this
     * applies.
     */
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
