val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val flyway_version: String by project
val prometeus_version: String by project
val testcontainers_version: String by project

plugins {
    kotlin("jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.5"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

group = "com.reneruck"
version = "1.0.0"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:$ktor_version"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-default-headers-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-call-id-jvm")
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm")
    implementation("io.ktor:ktor-server-request-validation")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-jackson-jvm")
    implementation("io.ktor:ktor-serialization-jackson")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-mock")

    // DB
    implementation(platform("org.jdbi:jdbi3-bom:3.41.3"))
    implementation("org.jdbi:jdbi3-core")
    implementation("org.jdbi:jdbi3-postgres")
    implementation("com.h2database:h2:2.2.224")
    implementation("org.flywaydb:flyway-core:$flyway_version")
    implementation("org.flywaydb:flyway-database-postgresql:10.0.0")
    implementation("org.postgresql:postgresql:42.6.0")

    implementation("com.typesafe:config:1.4.3")
    implementation("com.github.f4b6a3:uuid-creator:5.3.5")

    implementation("io.micrometer:micrometer-registry-prometheus:$prometeus_version")

    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jdbi:jdbi3-testcontainers")

    testImplementation("org.testcontainers:testcontainers:$testcontainers_version")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainers_version")
    testImplementation("org.testcontainers:postgresql:$testcontainers_version")
}
