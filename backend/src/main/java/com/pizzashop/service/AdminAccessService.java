package com.pizzashop.service;

import com.pizzashop.dto.AdminAccessResponse;
import com.pizzashop.entity.AdminAccess;
import com.pizzashop.exception.InvalidCredentialsException;
import com.pizzashop.exception.ResourceNotFoundException;
import com.pizzashop.exception.ValidationException;
import com.pizzashop.repository.AdminAccessRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class AdminAccessService {

    private final AdminAccessRepository adminAccessRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccessService(AdminAccessRepository adminAccessRepository,
                              PasswordEncoder passwordEncoder) {
        this.adminAccessRepository = adminAccessRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Emails are stored and compared lower-cased: what someone types into the login form is not
     * guaranteed to match the casing used in the invite form, and treating
     * "Anna@example.com" as a different admin than "anna@example.com" would silently
     * lock people out.
     */
    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<AdminAccessResponse> getAdmins() {
        return adminAccessRepository.findAllByOrderByEmailAsc().stream()
                .map(admin -> new AdminAccessResponse(
                        admin.getEmail(), admin.getApprovedAt(), admin.getApprovedBy()))
                .toList();
    }

    /**
     * Creates a new admin with an initial password chosen by the inviting admin. There is no
     * mail delivery in this application, so the password is handed over out of band; the invited
     * person is expected to change it via {@link #changePassword} afterwards.
     */
    public AdminAccessResponse invite(String email, String rawPassword, String approvedBy) {
        String normalized = normalize(email);
        if (adminAccessRepository.existsById(normalized)) {
            throw new ValidationException("This email is already an admin: " + normalized);
        }
        // approvedBy comes from the authentication, which is not guaranteed to carry a name;
        // record it as unknown rather than failing the invitation with an NPE.
        String approver = approvedBy == null ? "unknown" : normalize(approvedBy);
        AdminAccess saved = adminAccessRepository.save(
                new AdminAccess(normalized, approver, passwordEncoder.encode(rawPassword)));
        return new AdminAccessResponse(saved.getEmail(), saved.getApprovedAt(), saved.getApprovedBy());
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        AdminAccess admin = adminAccessRepository.findById(normalize(email))
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + email));

        if (admin.getPasswordHash() == null
                || !passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
            throw new InvalidCredentialsException("The current password is not correct.");
        }

        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        adminAccessRepository.save(admin);
    }

    /**
     * Ensures the bootstrap admin from the environment can sign in, solving the invite
     * chicken-and-egg problem at first start.
     *
     * <p>An existing password is never overwritten — otherwise every restart would silently
     * reset a password the admin had deliberately changed. The one thing this does repair is a
     * row left without a hash by the Google-login era, which could otherwise never sign in again.
     */
    public void ensureBootstrapAdmin(String email, String rawPassword) {
        String normalized = normalize(email);
        AdminAccess existing = adminAccessRepository.findById(normalized).orElse(null);

        if (existing == null) {
            adminAccessRepository.save(
                    new AdminAccess(normalized, "bootstrap", passwordEncoder.encode(rawPassword)));
        } else if (existing.getPasswordHash() == null) {
            existing.setPasswordHash(passwordEncoder.encode(rawPassword));
            adminAccessRepository.save(existing);
        }
    }
}
