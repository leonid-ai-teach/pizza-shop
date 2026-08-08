package com.pizzashop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An email address approved for the admin area. Access is granted by prior invitation
 * only — there is no self-registration and no "pending approval" state
 * (docs/adr/0001-google-oauth-admin-auth.md).
 */
@Entity
@Table(name = "admin_access")
public class AdminAccess {

    @Id
    @Column(nullable = false)
    private String email;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;

    @Column(name = "approved_by", nullable = false)
    private String approvedBy;

    protected AdminAccess() {
    }

    public AdminAccess(String email, String approvedBy) {
        this.email = email;
        this.approvedBy = approvedBy;
        this.approvedAt = Instant.now();
    }

    public String getEmail() {
        return email;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }
}
