-- Der Admin-Login laeuft nicht mehr ueber Google, sondern ueber E-Mail + Passwort
-- (docs/adr/0003-admin-password-auth.md). Die Allowlist-Tabelle wird damit zugleich die
-- Benutzertabelle.
--
-- Bewusst NULL-bar: Zeilen aus der OAuth-Zeit haben keinen Hash, und eine Zeile ohne Hash
-- kann sich schlicht nicht anmelden. So entsteht an keiner Stelle ein Default-Passwort.
--
-- VARCHAR(100) statt der 60 Zeichen eines reinen BCrypt-Hashes: Springs
-- DelegatingPasswordEncoder stellt dem Hash die verwendete Variante voran ("{bcrypt}$2a$10$...").
ALTER TABLE admin_access ADD COLUMN password_hash VARCHAR(100);
