plugins {
    kotlin("jvm") version "2.4.10"
}

group = "com.qupaya"
version = "v0.0.16"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

val keycloakVersion = "26.7.2"

// Keycloak supplies these at runtime. Keeping them off runtimeClasspath is what lets the jar task
// bundle everything it resolves instead of guessing by artifact name.
val providedByKeycloak: Configuration by configurations.creating
configurations.compileOnly { extendsFrom(providedByKeycloak) }
configurations.testImplementation { extendsFrom(providedByKeycloak) }

dependencies {
    providedByKeycloak("org.keycloak:keycloak-core:$keycloakVersion")
    providedByKeycloak("org.keycloak:keycloak-services:$keycloakVersion")
    providedByKeycloak("org.keycloak:keycloak-server-spi:$keycloakVersion")
    providedByKeycloak("org.keycloak:keycloak-server-spi-private:$keycloakVersion")
    providedByKeycloak("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")

    implementation("com.google.guava:guava:33.7.1-jre")
    implementation("org.json:json:20260814")
    implementation("org.apache.httpcomponents:httpmime:4.5.14")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")

    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
    testImplementation("com.squareup.okhttp3:mockwebserver3:5.5.0")
    testImplementation("org.glassfish.jersey.core:jersey-common:3.1.12")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().map { zipTree(it) } })
}
