package com.pizzashop.config;

import com.pizzashop.service.AdminAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Seeds the first admin from ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD. Without this,
 * nobody could ever log in to issue the first invitation
 * (docs/adr/0003-admin-password-auth.md).
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminAccessService adminAccessService;
    private final String bootstrapEmail;
    private final String bootstrapPassword;

    public AdminBootstrapRunner(AdminAccessService adminAccessService,
                                @Value("${pizzashop.admin.bootstrap-email:}") String bootstrapEmail,
                                @Value("${pizzashop.admin.bootstrap-password:}") String bootstrapPassword) {
        this.adminAccessService = adminAccessService;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Both or nothing: an admin row without a password could never sign in, and a password
        // without an email has nowhere to go.
        if (!StringUtils.hasText(bootstrapEmail) || !StringUtils.hasText(bootstrapPassword)) {
            log.warn("ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD are not both configured - "
                    + "no bootstrap admin was created, and nobody may be able to sign in to the "
                    + "admin area.");
            return;
        }
        adminAccessService.ensureBootstrapAdmin(bootstrapEmail, bootstrapPassword);
        log.info("Bootstrap admin ensured for {}", bootstrapEmail);
    }
}
