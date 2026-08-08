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
 * Seeds the first allowlisted admin from ADMIN_BOOTSTRAP_EMAIL. Without this, nobody could
 * ever log in to issue the first invitation (docs/adr/0001-google-oauth-admin-auth.md).
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminAccessService adminAccessService;
    private final String bootstrapEmail;

    public AdminBootstrapRunner(AdminAccessService adminAccessService,
                                @Value("${pizzashop.admin.bootstrap-email:}") String bootstrapEmail) {
        this.adminAccessService = adminAccessService;
        this.bootstrapEmail = bootstrapEmail;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(bootstrapEmail)) {
            log.warn("No ADMIN_BOOTSTRAP_EMAIL configured - the admin allowlist may be empty "
                    + "and nobody will be able to sign in to the admin area.");
            return;
        }
        adminAccessService.ensureBootstrapAdmin(bootstrapEmail);
        log.info("Bootstrap admin ensured for {}", bootstrapEmail);
    }
}
