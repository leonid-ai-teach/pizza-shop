#!/bin/sh
# Sichert die Datenbank in eine gzip-Datei und raeumt alte Sicherungen weg.
#
#   ./deploy/backup.sh                    # nach ./backups
#   BACKUP_DIR=/mnt/backups ./deploy/backup.sh
#
# Als taeglicher Cron-Eintrag (crontab -e), Pfad anpassen:
#   15 3 * * * cd /opt/pizza-shop && ./deploy/backup.sh >> /var/log/pizza-backup.log 2>&1
#
# Zurueckspielen — loescht den aktuellen Inhalt der Datenbank:
#   gunzip -c backups/pizzashop-2026-08-15-0315.sql.gz \
#     | docker compose exec -T db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"

set -eu

cd "$(dirname "$0")/.."

# Zugangsdaten und Datenbankname stehen in der .env, die auch Compose liest.
. ./.env

BACKUP_DIR="${BACKUP_DIR:-./backups}"
# Wie viele Tage Sicherungen liegen bleiben.
KEEP_DAYS="${KEEP_DAYS:-14}"

mkdir -p "$BACKUP_DIR"
target="$BACKUP_DIR/${POSTGRES_DB}-$(date +%F-%H%M).sql.gz"

# Erst in eine .part-Datei: bricht der Dump ab, bleibt keine halbe Sicherung liegen, die
# aussieht wie eine ganze.
docker compose exec -T db pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" | gzip > "$target.part"
mv "$target.part" "$target"
chmod 600 "$target"

find "$BACKUP_DIR" -name "${POSTGRES_DB}-*.sql.gz" -mtime "+$KEEP_DAYS" -delete

echo "$(date +%FT%T) Sicherung geschrieben: $target ($(du -h "$target" | cut -f1))"
