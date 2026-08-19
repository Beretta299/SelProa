plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    application
}

group = "selproa.registry"
version = "0.1.0"

application {
    mainClass.set("selproa.registry.ApplicationKt")
}

kotlin { jvmToolchain(21) }

repositories { mavenCentral() }

dependencies {
    // The BOM keeps every ktor artifact on one version.
    implementation(platform("io.ktor:ktor-bom:3.2.0"))

    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-websockets")

    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("org.flywaydb:flyway-core:11.8.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.8.2")
    implementation("ch.qos.logback:logback-classic:1.5.18")

    testImplementation(platform("io.ktor:ktor-bom:3.2.0"))
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

// ./gradlew seed  — regenerates the market. Deterministic on SEED in Seed.kt.
tasks.register<JavaExec>("seed") {
    group = "application"
    description = "Generate the mock market"
    mainClass.set("selproa.registry.SeedKt")
    classpath = sourceSets["main"].runtimeClasspath
    environment("REGISTRY_DB_URL", System.getenv("REGISTRY_DB_URL") ?: "jdbc:postgresql://localhost:5433/registry")
}
