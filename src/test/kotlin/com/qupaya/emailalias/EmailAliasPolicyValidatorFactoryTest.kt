package com.qupaya.emailalias

import org.keycloak.provider.ConfiguredProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class EmailAliasPolicyValidatorFactoryTest {
    @Test
    fun `factory is a ConfiguredProvider so the admin console lists the validator`() {
        val provider = assertIs<ConfiguredProvider>(EmailAliasPolicyValidatorFactory())

        assertFalse(provider.helpText.isBlank())
        assertEquals(emptyList(), provider.configProperties)
    }
}
