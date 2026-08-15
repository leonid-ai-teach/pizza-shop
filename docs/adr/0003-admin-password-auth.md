---
status: accepted
supersedes: 0001-google-oauth-admin-auth.md
---

# Admin-Anmeldung mit E-Mail und Passwort statt Google OAuth

[ADR 0001](0001-google-oauth-admin-auth.md) hat den Admin-Login über Google OAuth2 gelöst und lokale Accounts ausdrücklich als unnötigen Aufwand verworfen. Diese Entscheidung wird umgekehrt, weil sich die Anforderung geändert hat: Die Anwendung soll ins Web deployt werden, und Google OAuth stellt dafür drei Vorbedingungen, die vorher nicht ins Gewicht fielen. Google akzeptiert als Redirect-URI ausschließlich `https://` (einzige Ausnahme ist `localhost`) — ohne eigene Domain und ein gültiges Zertifikat lässt sich die Anwendung also gar nicht erst online testen. Die konkrete URL wird damit zu einem Stück Konfiguration: Jede Umgebung braucht ihren eigenen Eintrag in der Google Cloud Console, und ein Wechsel des Hosters oder eine zusätzliche Staging-Instanz bedeutet jedes Mal Handarbeit in einer fremden Oberfläche. Schließlich ist der OAuth-Login ein Browser-Redirect, der die Anwendung verlässt und zurückkommt, was eine Aufteilung von Frontend und Backend auf getrennte Hoster unnötig sperrig macht.

Die Anmeldung läuft jetzt über `POST /api/admin/login` mit E-Mail und Passwort und legt wie bisher eine Server-Session an. Die bestehende `AdminAccess`-Tabelle wird um eine Spalte `password_hash` erweitert und ist damit zugleich Einladungsliste und Benutzertabelle — es gibt weiterhin nur eine Rolle, eine separate `User`-Entität würde keine zusätzliche Information tragen. Gehasht wird mit Springs `DelegatingPasswordEncoder`, der die verwendete Variante im Hash vermerkt (`{bcrypt}$2a$10$…`), sodass ein späterer Wechsel des Verfahrens die vorhandenen Passwörter nicht auf einen Schlag entwertet. Das Einladungsmodell bleibt unverändert: Ein bestehender Admin legt die neue Person samt erstem Passwort an und gibt es außerhalb der Anwendung weiter, denn es gibt keinen Mailversand; die eingeladene Person ändert es anschließend selbst über `PUT /api/admin/me/password`. Der erste Admin kommt weiterhin aus der Umgebung, jetzt aus `ADMIN_BOOTSTRAP_EMAIL` **und** `ADMIN_BOOTSTRAP_PASSWORD`.

Session-Cookie, CSRF-Schutz über das `XSRF-TOKEN`-Cookie, der 401-Entry-Point und der `adminAuthGuard` im Frontend bleiben unverändert — sie hingen nie am Identitätsanbieter.

## Considered Options

- **Google OAuth beibehalten und um einen Passwort-Login ergänzen**: hätte den sichereren Weg erhalten, aber zwei parallele Anmeldepfade in Code, Tests und Dokumentation bedeutet — genau die Komplexität, die mit dem Umbau abgebaut werden sollte.
- **Zustandsloses Token (JWT) im `Authorization`-Header statt Session**: würde erlauben, das Frontend kostenlos auf einem statischen CDN und das Backend woanders zu betreiben. Verworfen, weil der Nutzen gering ist (jede in Frage kommende Plattform kann die SPA am selben Origin ausliefern) und der Preis real: Token-Ablage im Browser ist eine XSS-Angriffsfläche, und ein sofortiges Logout wird unschärfer.
- **Ein einziges Admin-Passwort aus einer Umgebungsvariablen**: am wenigsten Code, hätte aber den vorhandenen Verwaltungsbereich für Admins samt Einladungsfunktion entwertet.

## Consequences

- **Die Anwendung verwaltet jetzt selbst Passwörter.** Das ist der eigentliche Preis dieser Entscheidung: Es gibt keine Zwei-Faktor-Authentisierung mehr, keinen fremden Schutz gegen Rateangriffe, und ein schwaches Passwort ist ein direktes Risiko. Abgesichert ist bisher nur die Mindestlänge von zehn Zeichen. Eine Sperre nach zu vielen Fehlversuchen fehlt und sollte nachgezogen werden, sobald die Anwendung öffentlich erreichbar ist.
- Der Login-Endpunkt ist bewusst von der CSRF-Prüfung ausgenommen: Er läuft, bevor eine Session existiert, trägt also keine Autorität, auf der ein Angreifer mitreiten könnte.
- Es gibt keinen Weg, ein vergessenes Passwort selbst zurückzusetzen. Ein anderer Admin kann nicht helfen, weil niemand fremde Passwörter setzen kann; im Zweifel bleibt der Weg über `ADMIN_BOOTSTRAP_*` und die Datenbank.
- `/dev-login` entfällt ersatzlos. Der Endpunkt existierte nur, um den Admin-Bereich ohne Google-Credentials öffnen zu können, und war mit dem Risiko erkauft, dass er niemals in eine deployte Umgebung gelangen darf. Lokal meldet man sich jetzt regulär mit den Werten aus `application-localdev.yml` an.
- Zeilen aus der OAuth-Zeit haben kein Passwort. Sie können sich nicht anmelden, bis eines gesetzt wird — an keiner Stelle entsteht ein Standardpasswort.
