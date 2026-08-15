package com.pizzashop.security;

import com.pizzashop.entity.AdminAccess;
import com.pizzashop.repository.AdminAccessRepository;
import com.pizzashop.service.AdminAccessService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Looks admins up in the {@code admin_access} table. That table is both the invitation list and
 * the user table: there is exactly one role, so a separate user entity would carry no extra
 * information (docs/adr/0003-admin-password-auth.md).
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminAccessRepository adminAccessRepository;

    public AdminUserDetailsService(AdminAccessRepository adminAccessRepository) {
        this.adminAccessRepository = adminAccessRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminAccess admin = adminAccessRepository.findById(AdminAccessService.normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException("No admin for " + username));

        // A row without a hash predates the switch away from Google login. It must not be
        // signable-in; refusing here is what keeps such a row from becoming a passwordless account.
        if (admin.getPasswordHash() == null) {
            throw new UsernameNotFoundException("Admin has no password set: " + username);
        }

        return User.withUsername(admin.getEmail())
                .password(admin.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(AdminPrincipals.ROLE_ADMIN)))
                .build();
    }
}
