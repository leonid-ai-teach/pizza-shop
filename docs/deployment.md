# Deployment — Schritt für Schritt

Wie der Pizza Shop ins Netz kommt: auf eine kleine Linux-Maschine, mit demselben
`docker compose`, das auch lokal startet — davor ein [Caddy](https://caddyserver.com/), der
HTTPS übernimmt. Danach rollt [GitHub Actions](https://github.com/features/actions) jede neue
Version selbst aus.

Beschrieben ist die **Oracle Cloud Always Free**-Maschine, weil sie dauerhaft nichts kostet.
Auf jedem anderen Ubuntu-Server (Hetzner, Netcup, eigener Rechner am Anschluss) sind nur die
Schritte 3 und 4 andere; alles übrige ist identisch.

Für die lokale Einrichtung siehe [README](../README.md), für die Technik dahinter die
[Entwickler-Doku](entwicklerdoku.md).

---

## Alle Links auf einen Blick

`DEIN-KONTO` überall durch den eigenen GitHub-Namen ersetzen, `pizza-shop` durch den
Repository-Namen, falls er anders heißt.

| Wofür | Link |
| :--- | :--- |
| Oracle-Konto anlegen | https://signup.oraclecloud.com/ |
| Oracle-Konsole | https://cloud.oracle.com/ |
| Instanz anlegen | https://cloud.oracle.com/compute/instances/create |
| Instanzen ansehen (IP ablesen) | https://cloud.oracle.com/compute/instances |
| Netzwerke → Security Lists | https://cloud.oracle.com/networking/vcns |
| Object Storage (für Sicherungen) | https://cloud.oracle.com/object-storage/buckets |
| Was Always Free genau umfasst | https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm |
| Neues GitHub-Repository | https://github.com/new |
| Actions-Läufe ansehen | https://github.com/DEIN-KONTO/pizza-shop/actions |
| Secrets anlegen | https://github.com/DEIN-KONTO/pizza-shop/settings/secrets/actions/new |
| Repository öffentlich schalten | https://github.com/DEIN-KONTO/pizza-shop/settings (ganz unten, „Danger Zone") |
| Eigene Pakete (Images) | https://github.com/DEIN-KONTO?tab=packages |
| Paket öffentlich schalten | https://github.com/users/DEIN-KONTO/packages/container/pizza-shop-backend/settings |
| Token für privaten Zugriff (nur Notlösung) | https://github.com/settings/tokens/new?scopes=read:packages&description=pizza-shop-server |
| Kostenlose Domain | https://www.duckdns.org/ |
| DNS-Eintrag prüfen (ohne Terminal) | https://dnschecker.org/ |
| Docker-Installationsskript | https://get.docker.com |
| Let's-Encrypt-Sperrfristen | https://letsencrypt.org/docs/rate-limits/ |
| Caddyfile-Syntax | https://caddyserver.com/docs/caddyfile |
| Welche Actions-Runner es gibt | https://docs.github.com/en/actions/reference/runners/github-hosted-runners |
| Git für Windows (falls noch nicht da) | https://git-scm.com/download/win |

---

## Was am Ende läuft

```
Browser ──HTTPS──> Caddy :443 ──HTTP──> nginx :80 ──/api──> Backend :8080 ──> PostgreSQL
                   (Zertifikat,          (SPA +                              (Volume pgdata)
                    HSTS)                 Reverse Proxy)

git push ──> GitHub Actions ──> Tests ──> Images bauen (ARM) ──> ghcr.io ──> Maschine zieht
```

Nach außen offen sind nur 80 und 443 am Caddy. nginx, Backend und Datenbank hängen im
internen Compose-Netz und veröffentlichen keinen einzigen Port.

| Datei | Rolle |
| :--- | :--- |
| [`docker-compose.prod.yml`](../docker-compose.prod.yml) | Aufsatz auf `docker-compose.yml`: Caddy davor, nginx ohne Host-Port |
| [`docker-compose.registry.yml`](../docker-compose.registry.yml) | zweiter Aufsatz: fertige Images ziehen statt selbst bauen |
| [`deploy/Caddyfile`](../deploy/Caddyfile) | Domain, Zertifikat, Sicherheits-Header |
| [`deploy/remote-update.sh`](../deploy/remote-update.sh) | ein Ausrollvorgang: ziehen, starten, nachsehen ob es lebt |
| [`deploy/backup.sh`](../deploy/backup.sh) | `pg_dump` mit Aufräumen alter Sicherungen |
| [`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml) | Tests, Images, Ausrollen |
| [`.env.prod.example`](../.env.prod.example) | Vorlage für die `.env` auf der Maschine |

**Zeitbedarf:** Teil A etwa eine Stunde (das meiste ist Warten auf Oracle), Teil B eine
Viertelstunde. Danach dauert ein Deployment ungefähr fünf Minuten und läuft von allein.

Weil TLS vor der Anwendung endet, muss das Backend erfahren, dass der Browser `https`
gesprochen hat — sonst verlieren `JSESSIONID` und `XSRF-TOKEN` ihr `Secure`-Flag. Dafür sorgen
zwei Stellen: der nginx reicht ein vorhandenes `X-Forwarded-Proto` durch, und der Prod-Aufsatz
stellt das Backend auf `SERVER_FORWARD_HEADERS_STRATEGY=native`. Begründet ist beides in der
[Entwickler-Doku](entwicklerdoku.md#betrieb-im-netz); zu tun ist nichts.

---

# Teil A — Die Maschine einrichten

## Schritt 1: Was du vorher brauchst

- Ein **GitHub-Konto** → https://github.com/signup
- Ein **Oracle-Cloud-Konto** → https://signup.oraclecloud.com/
  (verlangt eine Kreditkarte zur Identitätsprüfung; das Always-Free-Kontingent kostet trotzdem
  nichts)
- Eine **Domain**, oder einen kostenlosen Namen bei https://www.duckdns.org/
- Einen **SSH-Schlüssel** auf dem eigenen Rechner. Falls noch keiner da ist, in PowerShell:

```powershell
ssh-keygen -t ed25519 -C "pizza-shop"
```

Zweimal Enter (Passphrase optional). Es entstehen `~/.ssh/id_ed25519` (geheim, bleibt hier)
und `~/.ssh/id_ed25519.pub` (öffentlich, kommt in Schritt 3 auf die Maschine). Den öffentlichen
Teil gleich in die Zwischenablage legen:

```powershell
Get-Content $HOME\.ssh\id_ed25519.pub | Set-Clipboard
```

## Schritt 2: Repository auf GitHub anlegen

Der Workflow lebt im Repository, also muss der Code dorthin.

1. Neues Repository anlegen → **https://github.com/new**, Name z. B. `pizza-shop`.
   **Empfehlung: öffentlich.** Die ARM-Runner, die der Workflow zum Bauen braucht, sind für
   öffentliche Repositories kostenlos
   ([Übersicht der Runner](https://docs.github.com/en/actions/reference/runners/github-hosted-runners));
   in einem privaten kosten sie Minuten aus dem
   [Monatskontingent](https://docs.github.com/en/billing/managing-billing-for-your-products/managing-billing-for-github-actions/about-billing-for-github-actions).
   Umstellen geht später jederzeit unter
   `https://github.com/DEIN-KONTO/pizza-shop/settings` ganz unten.
2. Lokal verbinden und hochladen:

```bash
git remote add origin https://github.com/DEIN-KONTO/pizza-shop.git
git push -u origin master
```

> **Prüfen:** Unter **https://github.com/DEIN-KONTO/pizza-shop/actions** läuft jetzt der erste
> Workflow und macht die Tests. Bauen und Ausrollen überspringt er noch — dafür fehlen die
> Secrets aus Teil B.

## Schritt 3: Maschine bei Oracle anlegen

Direkt zum Formular: **https://cloud.oracle.com/compute/instances/create**
(Klickweg, falls sich die Adresse ändert: Menü → *Compute* → *Instances* → *Create instance*.)

Die **Heimatregion lässt sich später nicht mehr ändern** — eine europäische Region wählen,
wenn die Gäste hier bestellen.

| Feld | Wert |
| :--- | :--- |
| Image | Canonical Ubuntu 24.04 |
| Shape | `VM.Standard.A1.Flex` (Ampere, ARM) |
| OCPUs / Memory | **2 / 12 GB** |
| Boot volume | Vorgabe genügt; bis 200 GB sind frei |
| SSH key | *Paste public key* — der Inhalt aus der Zwischenablage von Schritt 1 |
| Public IPv4 | zuweisen lassen |

> **2 OCPU / 12 GB, nicht mehr.** Oracle hat das Always-Free-Kontingent zum 15.06.2026 von
> 4/24 halbiert und terminiert ab dem 18.08.2026 Instanzen, die darüber liegen. Nachzulesen in
> der [Always-Free-Doku](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)
> („equivalent to 2 OCPUs and 12 GB of memory"). Für diesen Stack reicht das mit großem Abstand
> (PostgreSQL ~256 MB, JVM ~512 MB).

**„Out of host capacity"** ist bei der ARM-Shape normal und kein Fehler. Dann eine andere
Availability Domain wählen oder es zu einer anderen Tageszeit erneut versuchen.

Die öffentliche IP steht danach in der Instanzenliste → **https://cloud.oracle.com/compute/instances**

> **Prüfen:** Die Anmeldung klappt.
> ```bash
> ssh ubuntu@<ip>
> ```

## Schritt 4: Ports öffnen

Zwei Hürden, beide müssen fallen — das ist die häufigste Ursache für „die Seite lädt nicht".

**a) In der Cloud:** **https://cloud.oracle.com/networking/vcns** → das VCN anklicken →
*Security Lists* → *Default Security List* → *Add Ingress Rules*. Zweimal anlegen:

| Feld | Regel 1 | Regel 2 |
| :--- | :--- | :--- |
| Source CIDR | `0.0.0.0/0` | `0.0.0.0/0` |
| IP Protocol | TCP | TCP |
| Destination Port Range | `80` | `443` |

**b) Auf der Maschine:** Die Ubuntu-Images von Oracle bringen eigene iptables-Regeln mit, die
alles außer SSH verwerfen. Auf der Maschine (`ssh ubuntu@<ip>`) am Stück einfügen:

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80  -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

## Schritt 5: Docker installieren

Das offizielle Skript von https://get.docker.com (Hintergrund:
[Docker-Doku für Ubuntu](https://docs.docker.com/engine/install/ubuntu/)):

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
exit                              # neu anmelden, damit die Gruppe greift
```

> **Prüfen:** Nach erneutem `ssh` muss `docker compose version` **2.24 oder neuer** melden.
> Ältere Versionen kennen das `!override` aus `docker-compose.prod.yml` nicht.

## Schritt 6: Domain eintragen

Ein **A-Record** der Domain auf die öffentliche IP der Maschine.

- Eigene Domain: im DNS-Bereich des Anbieters, Typ `A`, Wert = die IP.
- Ohne eigene Domain: auf **https://www.duckdns.org/** mit GitHub anmelden, einen Namen
  eintragen (z. B. `pizza-beispiel`) und die IP ins Feld *current ip* schreiben. Die Domain
  heißt dann `pizza-beispiel.duckdns.org`.

> **Prüfen** — und zwar bevor es weitergeht, sonst bekommt Caddy kein Zertifikat und rennt in
> die [Sperrfrist von Let's Encrypt](https://letsencrypt.org/docs/rate-limits/):
> ```bash
> dig +short pizza.example.com     # muss die IP der Maschine zeigen
> ```
> Ohne Terminal geht es auch im Browser: https://dnschecker.org/

## Schritt 7: Erste Inbetriebnahme von Hand

Dieser erste Start baut die Images auf der Maschine selbst. Das dauert ein paar Minuten, ist
aber die beste Reihenfolge zum Suchen von Fehlern: Ports, DNS und Zertifikat sind danach
bewiesen, bevor in Teil B die Automatik obendrauf kommt.

Auf der Maschine, `DEIN-KONTO` anpassen:

```bash
sudo mkdir -p /opt/pizza-shop && sudo chown ubuntu:ubuntu /opt/pizza-shop
git clone https://github.com/DEIN-KONTO/pizza-shop.git /opt/pizza-shop
cd /opt/pizza-shop
```

Die `.env` musst du nicht von Hand zusammenbauen — dieser Block fragt die drei Angaben ab und
erzeugt die Passwörter gleich mit:

```bash
read -p "Domain (z. B. pizza.example.com): " DOMAIN
read -p "Deine E-Mail (für Let's Encrypt): " MAIL
read -p "E-Mail des ersten Admins: "        ADMIN

cat > .env <<EOF
COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml
PUBLIC_DOMAIN=$DOMAIN
ACME_EMAIL=$MAIL
POSTGRES_DB=pizzashop
POSTGRES_USER=pizzashop
POSTGRES_PASSWORD=$(openssl rand -base64 24)
ADMIN_BOOTSTRAP_EMAIL=$ADMIN
ADMIN_BOOTSTRAP_PASSWORD=$(openssl rand -base64 18)
EOF
chmod 600 .env

echo; echo "Damit meldest du dich gleich unter /admin an:"
grep ADMIN_BOOTSTRAP .env
```

**Die beiden ausgegebenen Zeilen wegspeichern** — das Admin-Passwort steht danach nur noch in
dieser Datei. Alle Felder samt Erklärung stehen in
[`.env.prod.example`](../.env.prod.example).

Dann starten. Ein `-f` braucht es nicht: welche Compose-Dateien gelten, steht als
`COMPOSE_FILE` in der `.env`.

```bash
docker compose up -d --build
```

Der erste Lauf dauert einige Minuten — Maven und der Angular-Build laufen auf der Maschine.
Alle verwendeten Images (`postgres:18-alpine`, `eclipse-temurin:21-jre-alpine`,
`nginx:1.29-alpine`, `node:22-alpine`, `caddy:2-alpine`) gibt es für ARM64, es ist also nichts
umzustellen.

> **Prüfen:** `https://deine-domain` öffnet die Speisekarte, `https://deine-domain/admin` nimmt
> die Anmeldedaten von oben an. Hängt etwas, sagen die Logs warum:
> ```bash
> docker compose logs -f caddy backend
> ```

---

# Teil B — Automatisch ausrollen

Ab hier baut GitHub die Images (auf ARM-Runnern, passend zur Maschine), lädt sie in die GitHub
Container Registry und startet den Stack neu. Die Maschine baut nichts mehr selbst.

## Schritt 8: Deploy-Schlüssel anlegen

GitHub braucht einen **eigenen** Schlüssel — nicht den persönlichen aus Schritt 1. Alles
folgende in PowerShell auf dem eigenen Rechner; die IP einmal setzen, dann laufen die Befehle
ohne weitere Anpassung durch:

```powershell
$IP = "<ip-der-maschine>"

ssh-keygen -t ed25519 -C "github-actions" -f $HOME\.ssh\pizza_deploy -N '""'

Get-Content $HOME\.ssh\pizza_deploy.pub |
  ssh ubuntu@$IP "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

Dann den Fingerabdruck der Maschine holen — über die bestehende, vertrauenswürdige Verbindung,
nicht per `ssh-keyscan` ins Blaue. Dieser Block baut die fertige `known_hosts`-Zeile und legt
sie in die Zwischenablage:

```powershell
$key   = ssh ubuntu@$IP "cat /etc/ssh/ssh_host_ed25519_key.pub"
$parts = $key.Split(' ')
"$IP $($parts[0]) $($parts[1])" | Set-Clipboard
```

## Schritt 9: Secrets in GitHub hinterlegen

Anlegen unter **https://github.com/DEIN-KONTO/pizza-shop/settings/secrets/actions/new** —
viermal, für jedes Secret einmal:

| Name | Inhalt | So kommst du dran |
| :--- | :--- | :--- |
| `DEPLOY_HOST` | die öffentliche IP der Maschine | https://cloud.oracle.com/compute/instances |
| `DEPLOY_USER` | `ubuntu` | — |
| `DEPLOY_SSH_KEY` | der **private** Schlüssel, samt `-----BEGIN…`- und `-----END…`-Zeile | `Get-Content $HOME\.ssh\pizza_deploy -Raw \| Set-Clipboard` |
| `DEPLOY_KNOWN_HOSTS` | die Zeile aus Schritt 8 | liegt schon in der Zwischenablage |

Der Workflow schreibt den Fingerabdruck bewusst aus dem Secret, statt ihn beim Verbinden
einzusammeln — sonst würde er jedem vertrauen, der unter der Adresse antwortet.

## Schritt 10: Images bauen lassen und freigeben

Einen Push auf `master` machen, oder den Lauf von Hand auslösen unter
**https://github.com/DEIN-KONTO/pizza-shop/actions/workflows/ci-cd.yml** → *Run workflow*.

Der Lauf testet, baut zwei Images und legt sie unter
**https://github.com/DEIN-KONTO?tab=packages** ab:

```
ghcr.io/dein-konto/pizza-shop-backend:latest
ghcr.io/dein-konto/pizza-shop-frontend:latest
```

Damit die Maschine sie ohne Anmeldung ziehen darf, **beide** Pakete öffentlich schalten:

- https://github.com/users/DEIN-KONTO/packages/container/pizza-shop-backend/settings
- https://github.com/users/DEIN-KONTO/packages/container/pizza-shop-frontend/settings

Dort ganz unten unter *Danger Zone* → *Change visibility* → **Public**. Die Images enthalten
keine Geheimnisse — die Konfiguration kommt zur Laufzeit aus der `.env`.

Wer sie lieber privat lässt, meldet die Maschine stattdessen einmalig an. Token erzeugen unter
**https://github.com/settings/tokens/new?scopes=read:packages&description=pizza-shop-server**
(die Berechtigung `read:packages` ist über den Link schon angehakt), dann auf der Maschine:

```bash
echo <TOKEN> | docker login ghcr.io -u DEIN-KONTO --password-stdin
```

## Schritt 11: Maschine auf die Registry umstellen

Auf der Maschine drei Zeilen in die `.env` schreiben — dieser Block erledigt es, `DEIN-KONTO`
vorher anpassen:

Der Kontoname gehört hier **klein geschrieben** hinein, auch wenn er auf GitHub große
Buchstaben hat — ghcr.io kennt nur Kleinbuchstaben.

```bash
cd /opt/pizza-shop
KONTO=dein-konto

sed -i "s|^COMPOSE_FILE=.*|COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml:docker-compose.registry.yml|" .env
cat >> .env <<EOF
BACKEND_IMAGE=ghcr.io/$KONTO/pizza-shop-backend:latest
FRONTEND_IMAGE=ghcr.io/$KONTO/pizza-shop-frontend:latest
EOF

grep -E "COMPOSE_FILE|IMAGE" .env      # zur Kontrolle
```

Ab jetzt liefert der Workflow die Compose-Dateien und die Skripte per `scp`; der Klon in
`/opt/pizza-shop` wird nicht mehr gebraucht. Wer ihn behält, sollte dort kein `git pull` mehr
machen — `git status` zeigt die überschriebenen Dateien sonst als Änderungen an.

## Schritt 12: Probelauf

```bash
git commit --allow-empty -m "Deployment testen"
git push
```

> **Prüfen:** Unter **https://github.com/DEIN-KONTO/pizza-shop/actions** laufen fünf Jobs durch
> — Backend-Tests, Frontend-Tests, zweimal Bauen, Ausrollen. Der letzte endet mit
> `Ausgerollt: https://… antwortet.` Genau dann hat das Skript die API nach dem Neustart
> erreicht; scheitert es, hängt es 60 Sekunden lang nach und gibt die letzten Log-Zeilen aus.

---

# Teil C — Im Betrieb

## Sicherungen

Die Bestelldaten liegen im Volume `pizza-shop_pgdata`. Es überlebt Neustarts und `down`, aber
weder `down -v` noch einen Verlust der Maschine. Täglich sichern:

```bash
crontab -e
```

Ans Ende der Datei:

```
15 3 * * * cd /opt/pizza-shop && ./deploy/backup.sh >> /var/log/pizza-backup.log 2>&1
```

Die Sicherungen landen in `backups/` (14 Tage Vorhaltezeit, über `BACKUP_DIR` und `KEEP_DAYS`
einstellbar). Damit sie einen Ausfall der Maschine überstehen, gehören sie regelmäßig
woandershin — im Always-Free-Kontingent bietet sich der
[Object Storage](https://cloud.oracle.com/object-storage/buckets) an (20 GB), sonst `rsync` auf
den eigenen Rechner. Das Zurückspielen steht im Kopf von
[`deploy/backup.sh`](../deploy/backup.sh); es **überschreibt** den aktuellen Stand, vorher also
erst eine frische Sicherung ziehen.

## Neue Version

`git push` auf `master` genügt. Von Hand geht es auch:

```bash
/opt/pizza-shop/deploy/remote-update.sh
```

Flyway wandert beim Start des Backends automatisch auf den neuen Schemastand. Vor einer
Migration, die Daten anfasst, erst `./deploy/backup.sh` laufen lassen.

## Zurück auf eine ältere Version

Jeder Lauf legt neben `:latest` auch einen Tag mit dem Commit-Hash ab; die Liste steht unter
`https://github.com/DEIN-KONTO/pizza-shop/pkgs/container/pizza-shop-backend`. In der `.env`
beide Bildnamen auf diesen Hash setzen und neu starten:

```
BACKEND_IMAGE=ghcr.io/dein-konto/pizza-shop-backend:a22880f…
FRONTEND_IMAGE=ghcr.io/dein-konto/pizza-shop-frontend:a22880f…
```

```bash
cd /opt/pizza-shop && docker compose up -d
```

Das gilt für den Code. Eine Schemaänderung nimmt Flyway damit **nicht** zurück — dafür ist die
Sicherung da.

## Nachsehen, was los ist

```bash
cd /opt/pizza-shop
docker compose ps                      # was läuft
docker compose logs -f backend         # Anwendungslog
docker compose logs --tail 50 caddy    # Zertifikat, HTTP-Fehler
df -h /                                # Platte
```

## Was im Blick bleiben sollte

| Thema | Was zu wissen ist |
| :--- | :--- |
| **Leerlauf** | Oracle zieht ungenutzte Always-Free-Instanzen ein. Eine Anwendung mit echtem Verkehr ist unkritisch, eine schlafende Demo kann verschwinden. |
| **Datenübertragung** | 10 TB ausgehend pro Monat sind frei — für diesen Shop unerreichbar. |
| **SSH** | Die Default Security List erlaubt Port 22 aus dem ganzen Netz. Besser auf die eigene IP einschränken oder `fail2ban` installieren. |
| **Geheimnisse** | Die `.env` liegt nur auf der Maschine, steht in `.gitignore` und sollte `chmod 600` bleiben. Der Workflow fasst sie nie an. |
| **Zertifikat** | Erneuert Caddy selbst. Es liegt im Volume `caddy_data` — bleibt das erhalten, kostet ein Neustart keinen neuen Antrag. |

---

## Wenn es klemmt

| Symptom | Ursache und wo du nachsiehst |
| :--- | :--- |
| Seite lädt gar nicht | Ports: [Security List](https://cloud.oracle.com/networking/vcns) **und** iptables (Schritt 4) — meist fehlt das zweite |
| Caddy meldet ACME-Fehler | A-Record zeigt nicht auf die Maschine (https://dnschecker.org/), oder Port 80 ist von außen zu |
| Admin-Login schlägt fehl, obwohl das Passwort stimmt | Das Passwort aus der `.env` gilt nur beim ersten Anlegen; danach zählt das geänderte. Weg zurück: [Entwickler-Doku](entwicklerdoku.md#admin_bootstrap_password-gilt-nur-beim-anlegen) |
| Actions bricht bei „Ausrollen" ab | [Secrets](https://github.com/DEIN-KONTO/pizza-shop/settings/secrets/actions) prüfen; `DEPLOY_KNOWN_HOSTS` muss zur IP passen. Nach einem Neuaufsetzen der Maschine ändert sich der Fingerabdruck |
| Maschine zieht die Images nicht (`denied`) | Pakete stehen noch auf privat → https://github.com/DEIN-KONTO?tab=packages, Schritt 10 |
| „no matching manifest for linux/arm64" | ein Image ohne ARM-Variante wurde ergänzt — Alternative suchen oder auf eine AMD-Shape wechseln (dann nicht mehr Always Free) |
| Build bricht mit „no space left" ab | `docker system prune -af`, danach das Boot-Volume vergrößern |
| `!override` wird nicht erkannt | Compose ist älter als 2.24 — Docker über Schritt 5 neu installieren |

---

## Warum diese Variante

Zur Auswahl standen im August 2026 vor allem drei Wege:

* **Eigene kleine Maschine** (hier beschrieben) — das vorhandene `docker-compose.yml` **ist**
  das Deployment-Artefakt, alles bleibt unter einem Origin, die Daten liegen dauerhaft. Preis:
  man betreut einen Server.
* **[Northflank Free](https://northflank.com/pricing)** — 2 Services und eine Datenbank,
  dauerhaft an, kein Server-Betrieb. Verlangt aber, den Reverse-Proxy-Aufbau aufzutrennen (der
  `proxy_pass` müsste auf den internen Hostnamen des Backend-Service zeigen).
* **[Cloud Run](https://cloud.google.com/run/pricing) + [Neon](https://neon.tech/pricing)** —
  skaliert auf null und ist bei echter Last am günstigsten, verlangt aber ein kombiniertes
  Image oder getrennte Origins samt CORS- und Cookie-Anpassung, dazu mehrere Sekunden
  JVM-Kaltstart bei der ersten Bestellung.

Nicht geeignet: **[Render Free](https://render.com/docs/free)** (die kostenlose Datenbank wird
[nach 30 Tagen gelöscht](https://render.com/changelog/free-postgresql-instances-now-expire-after-30-days-previously-90),
der Dienst schläft nach 15 Minuten ein), **[Fly.io](https://fly.io/docs/about/pricing/)** und
**[Railway](https://railway.com/pricing)** (kein kostenloses Kontingent mehr) sowie
**[AWS](https://aws.amazon.com/free/)** (seit Juli 2025 nur noch befristete Guthaben).

Sollte Oracle das Kontingent weiter zusammenstreichen: Dieselbe Anleitung läuft ab Schritt 5
unverändert auf einem VPS für wenige Euro im Monat. Schritt 4b entfällt dort meist.
