package com.qupaya.blacklist

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class RemotePasswordBlacklistPolicyProviderFactoryTest {

    @Test
    fun `successfully resolve a password blacklist`() {
        val blacklist = resolve(listOf(200 to "password\n123456"))

        assertNotNull(blacklist, "There should be a blacklist")
        assertTrue(blacklist.contains("password"), "The blacklist should contain the given words")
        assertFalse(blacklist.contains("awesome-password"), "The blacklist should not contain words that are not given")
    }

    @Test
    fun `successfully resolve a two password blacklists`() {
        val blacklist = resolve(listOf(200 to "password\n123456", 200 to "hidden\nunguessable"))

        assertNotNull(blacklist, "There should be a blacklist")
        assertTrue(blacklist.contains("password"), "The blacklist should contain the words from first blacklist")
        assertTrue(blacklist.contains("unguessable"), "The blacklist should contain the words from the second blacklist")
        assertFalse(blacklist.contains("awesome-password"), "The blacklist should not contain words that are not given")
    }

    @Test
    fun `return null when the blacklist is not available`() {
        assertNull(resolve(listOf(404 to null)))
    }

    @Test
    fun `the blacklist reading should work case insensitive`() {
        val blacklist = resolve(listOf(200 to "PaSsWoRd"))

        assertNotNull(blacklist, "There should be a blacklist")
        assertTrue(blacklist.contains("pAsSwOrD"), "The blacklist should contain the given case insensitive words")
    }

    private fun resolve(responses: List<Pair<Int, String?>>): BlacklistResolver.PasswordBlacklist? =
        MockWebServer().use { server ->
            server.start()
            responses.forEach { (code, body) ->
                server.enqueue(
                    MockResponse.Builder()
                        .code(code)
                        .apply { if (body != null) body(body) }
                        .build(),
                )
            }
            val addresses = responses.indices.joinToString(" ") { server.url("/blacklist$it.txt").toString() }
            RemotePasswordBlacklistPolicyProviderFactory().resolvePasswordBlacklist(addresses)
        }
}
