-- Allowlist of Google accounts permitted into the admin area (docs/adr/0001-google-oauth-admin-auth.md).
-- Email is the natural, stable key; there is no local user/password record and only one role.
CREATE TABLE admin_access (
    email       VARCHAR(255) PRIMARY KEY,
    approved_at TIMESTAMP    NOT NULL,
    approved_by VARCHAR(255) NOT NULL
);
