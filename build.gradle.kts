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

dependencies {
    implementation("org.keycloak:keycloak-core:26.7.2")
    implementation("org.keycloak:keycloak-services:26.7.2")
    implementation("org.keycloak:keycloak-server-spi:26.7.2")
    implementation("org.keycloak:keycloak-server-spi-private:26.7.2")
    implementation("com.google.guava:guava:33.7.1-jre")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    implementation("org.json:json:20260814")
    implementation("org.apache.httpcomponents:httpmime:4.5.14")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")

    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation("org.glassfish.jersey.core:jersey-common:3.1.12")
}

tasks.test {
    useJUnitPlatform()
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
                    && (it.name.contains("guava")
                    || it.name.contains("json")
                    || it.name.contains("mime")
                    || it.name.contains("http")
                    || it.name.contains("kotlin"))

        }.map { zipTree(it) }
    })
}
