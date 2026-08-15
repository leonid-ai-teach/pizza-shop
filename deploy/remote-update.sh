#!/bin/sh
# Rollt den aktuellen Stand aus: neue Images holen, Stack neu starten, nachsehen ob er lebt.
#
# Ruft GitHub Actions ueber SSH auf (.github/workflows/ci-cd.yml), laesst sich aber genauso
# von Hand starten:
#
#   /opt/pizza-shop/deploy/remote-update.sh
#
# Welche Compose-Dateien gelten, steht als COMPOSE_FILE in der .env — deshalb kommen die
# Aufrufe hier ohne -f aus.

set -eu

cd "$(dirname "$0")/.."

# PUBLIC_DOMAIN fuer die Schlusspruefung. Compose liest die .env selbst.
. ./.env

docker compose pull
docker compose up -d

# Der Caddy haelt seine Konfiguration als Bind-Mount; eine geaenderte Caddyfile allein bewegt
# ihn also zu nichts. Ein Reload uebernimmt sie ohne Unterbrechung. Laeuft er (noch) nicht,
# ist das kein Grund abzubrechen — "up -d" hat ihn dann gerade erst gestartet.
docker compose exec -T caddy caddy reload --config /etc/caddy/Caddyfile || true

# Ausgerollt ist erst, was auch antwortet. Das Backend braucht nach einem Neustart ein paar
# Sekunden, bis Flyway durch ist und Tomcat annimmt.
tries=0
until curl -fsS -o /dev/null "https://${PUBLIC_DOMAIN}/api/pizzas"; do
    tries=$((tries + 1))
    if [ "$tries" -ge 30 ]; then
        echo "FEHLER: https://${PUBLIC_DOMAIN}/api/pizzas antwortet auch nach 60 s nicht." >&2
        docker compose logs --tail 50 backend caddy >&2
        exit 1
    fi
    sleep 2
done

# Alte Layer sammeln sich sonst an, bis das Boot-Volume voll ist.
docker image prune -f >/dev/null

echo "Ausgerollt: https://${PUBLIC_DOMAIN} antwortet."
