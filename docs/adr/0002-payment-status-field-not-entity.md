---
status: accepted
---

# Schlankes Payment-Status-Feld statt eigener Payment-Entität in V1

Der Master-Prompt fordert eine strikte Entkopplung von Order und Payment (`NOT_REQUIRED` als initialer Zahlungsstatus), was auf eine eigenständige `Payment`-Entität/Tabelle hindeutet. Für V1 gibt es jedoch keine Online-Zahlung und keine wachsenden Zahlungsdaten (Betrag, Provider, Transaktions-ID) — eine leere Aggregat-Tabelle für ein noch nicht existierendes Feature wäre verfrühte Abstraktion. Wir speichern daher in V1 nur ein `paymentStatus`-Enum-Feld (Wert `NOT_REQUIRED`) direkt auf `Order`. Die volle Entkopplung in eine eigene `Payment`-Tabelle wird erst eingeführt, wenn eine echte Zahlungsanbieter-Integration (V2) ansteht — das erfordert dann eine Migration von `Order.paymentStatus` zu einer eigenständigen `Payment`-Entität.
