package com.reneruck.routes.services

import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class DatabaseContainer {

    val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:16"))
        .withDatabaseName("somedatabasename")
        .withUsername("postgres")
        .withPassword("postgres")
        .also { it.start() }
        .also {
            Flyway.configure().dataSource(it.getJdbcUrl(), it.username, it.password).load().migrate()
        }

    val dbConnection = Jdbi.create(postgresContainer.getJdbcUrl(), postgresContainer.username, postgresContainer.password)
}
