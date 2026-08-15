package com.pizzashop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An admin of the shop, identified by email address. Access is granted by prior invitation
 * only — there is no self-registration and no "pending approval" state
 * (docs/adr/0003-admin-password-auth.md).
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

    /**
     * Null for rows created before the switch away from Google login. Such an admin simply
     * cannot sign in until a password is set; nothing anywhere invents a default one.
     */
    @Column(name = "password_hash")
    private String passwordHash;

    protected AdminAccess() {
    }

    public AdminAccess(String email, String approvedBy, String passwordHash) {
        this.email = email;
        this.approvedBy = approvedBy;
        this.passwordHash = passwordHash;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
