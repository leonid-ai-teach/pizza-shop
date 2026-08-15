# syntax=docker/dockerfile:1

# --- Build ------------------------------------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /build

# Erst der Lockfile-Layer: solange sich die Abhaengigkeiten nicht aendern, entfaellt das
# erneute Installieren bei jeder Codeaenderung.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Runtime ----------------------------------------------------------------------------
# nginx liefert die fertige SPA aus und ist zugleich Reverse Proxy zum Backend, damit beides
# unter einem Origin erreichbar ist (siehe nginx.conf).
FROM nginx:1.29-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
