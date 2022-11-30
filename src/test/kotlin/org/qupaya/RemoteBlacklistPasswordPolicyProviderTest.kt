package org.qupaya

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.keycloak.models.KeycloakContext
import org.keycloak.models.PasswordPolicy
import org.keycloak.models.RealmModel
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

internal class RemoteBlacklistPasswordPolicyProviderTest {

    @Nested
    inner class ParseConfig {
        private val blacklist = mock<BlacklistResolver.PasswordBlacklist> {}
        private val resolver = object : BlacklistResolver {
            override fun resolvePasswordBlacklist(blacklistAddress: String) = blacklist
        }
        private val context = createContext(null)

        @Test
        fun `return null in parseConfig when address is null`() {
            assertNull(RemoteBlacklistPasswordPolicyProvider(resolver, context).parseConfig(null))
        }

        @Test
        fun `return a configuration in parseConfig when address is good`() {
            assertSame(blacklist, RemoteBlacklistPasswordPolicyProvider(resolver, context).parseConfig("https://example.com/blacklist.txt"))
        }
    }

    @Nested
    inner class Validate {
        private val resolver = mock<BlacklistResolver> {
            on { resolvePasswordBlacklist(any()) } doReturn mock {}
        }

        @Test
        fun `validate without blacklist returns no error`() {
            val context = createContext(null)

            assertNull(RemoteBlacklistPasswordPolicyProvider(resolver, context).validate("", ""))
        }

        @Test
        fun `validate with with that is not in blacklist returns no error`() {
            val blacklist = mock<BlacklistResolver.PasswordBlacklist> {
                on { contains(any()) } doReturn false
            }
            val context = createContext(blacklist)

            assertNull(RemoteBlacklistPasswordPolicyProvider(resolver, context).validate("", "password"))
        }

        @Test
        fun `validate with with that is in blacklist returns error`() {
            val blacklist = mock<BlacklistResolver.PasswordBlacklist> {
                on { contains(eq("password")) } doReturn true
            }
            val context = createContext(blacklist)

            val error = RemoteBlacklistPasswordPolicyProvider(resolver, context).validate("", "password")
            assertEquals(RemoteBlacklistPasswordPolicyProvider.ERROR_MESSAGE, error?.message)
        }
    }

    fun createContext(blacklist: BlacklistResolver.PasswordBlacklist?): KeycloakContext {
        val pwPolicy = mock<PasswordPolicy> {
            on { getPolicyConfig<BlacklistResolver.PasswordBlacklist>(eq(RemoteBlacklistPasswordPolicyProviderFactory.ID)) } doReturn blacklist
        }
        val realmModel = mock<RealmModel> {
            on { passwordPolicy } doReturn pwPolicy
        }
        return mock {
            on { realm } doReturn realmModel
        }
    }
}