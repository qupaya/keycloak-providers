import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.qupaya"
version = "v0.0.16"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.keycloak:keycloak-core:26.0.7")
    implementation("org.keycloak:keycloak-services:26.0.7")
    implementation("org.keycloak:keycloak-server-spi:26.0.7")
    implementation("org.keycloak:keycloak-server-spi-private:26.0.7")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("javax.ws.rs:javax.ws.rs-api:2.1.1")
    implementation("org.json:json:20250107")
    implementation("org.apache.httpcomponents:httpmime:4.5.14")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")

    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation("org.glassfish.jersey.core:jersey-common:2.22.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Jar> {
    // To avoid the duplicate handling strategy error
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // To add all dependencies
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter {
            it.name.endsWith("jar")
                    && (it.name.contains("jbcrypt")
                    || it.name.contains("guava")
                    || it.name.contains("json")
                    || it.name.contains("mime")
                    || it.name.contains("http")
                    || it.name.contains("kotlin"))

        }.map { zipTree(it) }
    })
}
