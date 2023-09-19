import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
}

group = "com.qupaya"
version = "v0.0.9"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.keycloak:keycloak-core:20.0.3")
    implementation("org.keycloak:keycloak-services:20.0.3")
    implementation("org.keycloak:keycloak-server-spi:20.0.3")
    implementation("org.keycloak:keycloak-server-spi-private:20.0.3")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.mindrot:jbcrypt:0.4")

    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
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
                    && !it.name.contains("keycloak")
                    && !it.name.contains("jboss")
                    && !it.name.contains("resteasy")
                    && !it.name.contains("microprofile")
                    && !it.name.contains("smallrye")
        }.map { zipTree(it) }
    })
}
