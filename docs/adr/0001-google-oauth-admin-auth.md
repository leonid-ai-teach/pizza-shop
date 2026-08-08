---
status: accepted
---

# Admin-Authentifizierung über Google OAuth mit Einladungs-Allowlist

Der Admin-Bereich benötigt eine geschützte Anmeldung ohne eigene Passwortverwaltung. Wir haben uns für Login ausschließlich via Google OAuth2 entschieden, mit einer schlanken `AdminAccess`-Tabelle (E-Mail als Schlüssel, `approvedAt`, `approvedBy`) statt einer vollen `User`-Entität mit Rollen — es gibt nur eine einzige Rolle ("Admin"), und Google liefert Identitätsdaten (Name etc.) bei jedem Login per OIDC-Token, sodass sie nicht redundant gespeichert werden müssen.

Zugriff wird per Vorab-Einladung gewährt: Ein bestehender Admin trägt die E-Mail-Adresse eines neuen Admins proaktiv in die Allowlist ein, bevor sich diese Person das erste Mal einloggt — es gibt keinen unbestätigten "wartet auf Freigabe"-Zwischenzustand. Der allererste Admin wird über eine Umgebungsvariable (`ADMIN_BOOTSTRAP_EMAIL`) bzw. einen Flyway-Seed hinterlegt, um das Henne-Ei-Problem beim Start zu lösen. Die Google OAuth2 Client-ID/Secret werden einmalig manuell in der Google Cloud Console erzeugt und über `.env`/Umgebungsvariablen bereitgestellt — keine Automatisierung dieses Schritts im Projekt.

## Considered Options

- **Lokale Accounts mit Benutzername/Passwort (Spring Security Form Login)**: naheliegender Standardweg, aber erfordert eigene Passwort-Speicherung, Hashing, Reset-Flow und Nutzerverwaltung — unnötiger Aufwand, wenn alle Mitarbeiter ohnehin ein Google-Konto haben.

## Consequences

- Kein Zugriff auf den Admin-Bereich ohne Google-Konto — akzeptabel, da alle Mitarbeiter eines besitzen.
- Kein Self-Service-Registrierungsflow; jede Freigabe ist ein manueller Admin-Akt.
- Ein Wechsel des Auth-Providers später erfordert eine Migration der `AdminAccess`-Tabelle (E-Mail bleibt der stabile Schlüssel).
