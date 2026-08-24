package com.qupaya

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

data class SpiRegistration(val spiInterface: String, val factoryClass: String)

internal class SpiRegistrationTest {

    private val registrations: List<SpiRegistration> = serviceFiles().flatMap { file ->
        file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { SpiRegistration(spiInterface = file.name, factoryClass = it) }
    }

    @Test
    fun `every registration names a class Keycloak can load and instantiate`() {
        assertTrue(registrations.isNotEmpty(), "No SPI registrations were discovered, so this test would prove nothing")

        registrations.forEach { (spiInterface, factoryClass) ->
            val spi = load(spiInterface) ?: fail("The SPI interface $spiInterface does not exist")
            val factory = load(factoryClass)
                ?: fail("$spiInterface registers $factoryClass, which does not exist. Keycloak aborts startup when a registered class is missing.")

            assertTrue(spi.isAssignableFrom(factory), "$factoryClass is registered under $spiInterface but does not implement it")

            try {
                factory.getDeclaredConstructor().newInstance()
            } catch (ex: ReflectiveOperationException) {
                fail("$factoryClass is registered under $spiInterface but ServiceLoader cannot instantiate it, which aborts Keycloak startup the same way a missing class does: $ex")
            }
        }
    }

    @Test
    fun `every provider factory in the source tree is registered`() {
        val registered = registrations.map { it.factoryClass }.toSet()
        val declared = factoryClassNames()

        assertTrue(declared.isNotEmpty(), "No provider factories were found under $SOURCE_DIR, so this test would prove nothing")

        val unregistered = declared - registered
        assertTrue(
            unregistered.isEmpty(),
            "These provider factories exist but no META-INF/services file registers them, so Keycloak starts cleanly and the feature is simply absent: $unregistered",
        )
    }

    private fun load(className: String): Class<*>? =
        try {
            Class.forName(className, false, javaClass.classLoader)
        } catch (ex: ClassNotFoundException) {
            null
        }

    private fun serviceFiles(): List<File> {
        val services = File(SERVICES_DIR)
        assertTrue(services.isDirectory, "Expected $SERVICES_DIR relative to ${File("").absolutePath}, which is where Gradle runs tests from")
        return services.listFiles().orEmpty().filter { it.isFile }.sortedBy { it.name }
    }

    private fun factoryClassNames(): Set<String> {
        val root = File(SOURCE_DIR)
        assertTrue(root.isDirectory, "Expected $SOURCE_DIR relative to ${File("").absolutePath}")
        return root.walkTopDown()
            .filter { it.isFile && it.name.endsWith("ProviderFactory.kt") }
            .map { it.relativeTo(root).path.removeSuffix(".kt").replace(File.separatorChar, '.') }
            .toSet()
    }

    private companion object {
        const val SERVICES_DIR = "src/main/resources/META-INF/services"
        const val SOURCE_DIR = "src/main/kotlin"
    }
}
