import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
}

group = "com.qupaya"
version = "v0.0.12"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.keycloak:keycloak-core:24.0.5")
    implementation("org.keycloak:keycloak-services:24.0.5")
    implementation("org.keycloak:keycloak-server-spi:24.0.5")
    implementation("org.keycloak:keycloak-server-spi-private:24.0.5")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("javax.ws.rs:javax.ws.rs-api:2.1.1")


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
                    || it.name.contains("kotlin"))

        }.map { zipTree(it) }
    })
}
