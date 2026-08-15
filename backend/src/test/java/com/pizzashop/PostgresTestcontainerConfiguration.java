package com.pizzashop;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Startet das PostgreSQL, gegen das die Integrationstests laufen.
 *
 * <p>{@code @ServiceConnection} ueberschreibt die Datasource-Properties mit den Daten des
 * Containers, Spring uebernimmt dessen Lebenszyklus. Weil alle Integrationstests dieselbe
 * Kontext-Konfiguration tragen, cached Spring genau einen Kontext und damit auch genau
 * einen Container fuer den gesamten Testlauf.
 *
 * <p>Die Image-Version ist absichtlich dieselbe wie in {@code docker-compose.yml}: Tests
 * und Betrieb sollen nicht gegen unterschiedliche PostgreSQL-Versionen laufen.
 */
@TestConfiguration(proxyBeanMethods = false)
class PostgresTestcontainerConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
    }
}
