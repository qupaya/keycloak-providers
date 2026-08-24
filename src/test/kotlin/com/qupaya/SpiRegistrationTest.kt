package com.qupaya

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
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
    fun `every registered factory class exists and implements its SPI`() {
        assertTrue(registrations.isNotEmpty()) { "No SPI registrations were discovered, so this test would prove nothing" }

        registrations.forEach { (spiInterface, factoryClass) ->
            val spi = load(spiInterface) ?: fail("The SPI interface $spiInterface does not exist")
            val factory = load(factoryClass)
                ?: fail("$spiInterface registers $factoryClass, which does not exist. Keycloak aborts startup when a registered class is missing.")

            assertTrue(spi.isAssignableFrom(factory)) { "$factoryClass is registered under $spiInterface but does not implement it" }
        }
    }

    private fun load(className: String): Class<*>? =
        try {
            Class.forName(className, false, javaClass.classLoader)
        } catch (ex: ClassNotFoundException) {
            null
        }

    private fun serviceFiles(): List<File> {
        val services = File(SERVICES_DIR)
        assertTrue(services.isDirectory) { "Expected $SERVICES_DIR relative to ${File("").absolutePath}, which is where Gradle runs tests from" }
        return services.listFiles().orEmpty().filter { it.isFile }.sortedBy { it.name }
    }

    private companion object {
        const val SERVICES_DIR = "src/main/resources/META-INF/services"
    }
}
