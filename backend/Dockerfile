# syntax=docker/dockerfile:1

# --- Build ------------------------------------------------------------------------------
# Bewusst das Maven-Image statt ./mvnw: der Wrapper ist auf distributionType=only-script
# gestellt und wuerde bei jedem Image-Build die Maven-Distribution neu herunterladen.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Erst nur die pom.xml: solange sich die Abhaengigkeiten nicht aendern, bleibt dieser Layer
# gueltig und Codeaenderungen fuehren nicht zu einem erneuten Aufloesen.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline

COPY src ./src
# Tests laufen hier nicht: sie brauchen selbst einen Docker-Daemon (Testcontainers) und
# gehoeren in den Build der Entwicklungsmaschine bzw. in die CI, nicht in den Image-Build.
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

# --- Runtime ----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S pizzashop && adduser -S pizzashop -G pizzashop

WORKDIR /app
COPY --from=build /build/target/pizza-shop-backend-*.jar app.jar

USER pizzashop
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
