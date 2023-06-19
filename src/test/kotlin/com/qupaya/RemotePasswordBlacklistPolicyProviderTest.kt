package com.qupaya

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

internal class RemotePasswordBlacklistPolicyProviderTest {

    @Nested
    inner class ParseConfig {
        private val blacklist = mock<BlacklistResolver.PasswordBlacklist> {}
        private val resolver = object : BlacklistResolver {
            override fun resolvePasswordBlacklist(blacklistAddresses: String) = blacklist
        }
        private val context = createContext(null)

        @Test
        fun `return null in parseConfig when address is null`() {
            assertNull(RemotePasswordBlacklistPolicyProvider(resolver, context).parseConfig(null))
        }

        @Test
        fun `return a configuration in parseConfig when address is good`() {
            assertSame(blacklist, RemotePasswordBlacklistPolicyProvider(resolver, context).parseConfig("https://example.com/blacklist.txt"))
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

            assertNull(RemotePasswordBlacklistPolicyProvider(resolver, context).validate("", ""))
        }

        @Test
        fun `validate with word that is not in blacklist returns no error`() {
            val blacklist = mock<BlacklistResolver.PasswordBlacklist> {
                on { contains(any()) } doReturn false
            }
            val context = createContext(blacklist)

            assertNull(RemotePasswordBlacklistPolicyProvider(resolver, context).validate("", "password"))
        }

        @Test
        fun `validate with word that is in blacklist returns error`() {
            val blacklist = mock<BlacklistResolver.PasswordBlacklist> {
                on { contains(eq("password")) } doReturn true
            }
            val context = createContext(blacklist)

            val error = RemotePasswordBlacklistPolicyProvider(resolver, context).validate("", "password")
            assertEquals(RemotePasswordBlacklistPolicyProvider.ERROR_MESSAGE, error?.message)
        }
    }
}