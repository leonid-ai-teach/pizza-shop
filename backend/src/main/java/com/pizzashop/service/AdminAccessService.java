package com.pizzashop.service;

import com.pizzashop.dto.AdminAccessResponse;
import com.pizzashop.entity.AdminAccess;
import com.pizzashop.exception.ValidationException;
import com.pizzashop.repository.AdminAccessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class AdminAccessService {

    private final AdminAccessRepository adminAccessRepository;

    public AdminAccessService(AdminAccessRepository adminAccessRepository) {
        this.adminAccessRepository = adminAccessRepository;
    }

    /**
     * Emails are stored and compared lower-cased: the identity provider's casing is not
     * guaranteed to match what an admin typed into the invite form, and treating
     * "Anna@example.com" as a different admin than "anna@example.com" would silently
     * lock people out.
     */
    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public boolean isAllowed(String email) {
        return email != null && adminAccessRepository.existsById(normalize(email));
    }

    @Transactional(readOnly = true)
    public List<AdminAccessResponse> getAdmins() {
        return adminAccessRepository.findAllByOrderByEmailAsc().stream()
                .map(admin -> new AdminAccessResponse(
                        admin.getEmail(), admin.getApprovedAt(), admin.getApprovedBy()))
                .toList();
    }

    public AdminAccessResponse invite(String email, String approvedBy) {
        String normalized = normalize(email);
        if (adminAccessRepository.existsById(normalized)) {
            throw new ValidationException("This email is already an admin: " + normalized);
        }
        // approvedBy comes from the OIDC principal, which is not guaranteed to carry an email
        // claim; record it as unknown rather than failing the invitation with an NPE.
        String approver = approvedBy == null ? "unknown" : normalize(approvedBy);
        AdminAccess saved = adminAccessRepository.save(new AdminAccess(normalized, approver));
        return new AdminAccessResponse(saved.getEmail(), saved.getApprovedAt(), saved.getApprovedBy());
    }

    /** Inserts the bootstrap admin if absent, solving the invite chicken-and-egg problem at first start. */
    public void ensureBootstrapAdmin(String email) {
        String normalized = normalize(email);
        if (!adminAccessRepository.existsById(normalized)) {
            adminAccessRepository.save(new AdminAccess(normalized, "bootstrap"));
        }
    }
}
