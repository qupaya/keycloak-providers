package com.qupaya.blacklist

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class RemotePasswordBlacklistPolicyProviderFactoryTest {

    @Test
    fun `successfully resolve a password blacklist`() {
        val webServer = MockWebServer()
        webServer.start()
        webServer.enqueue(MockResponse.Builder()
            .code(200)
            .body("""
                password
                123456
            """.trimIndent())
            .build()
        )

        val blacklist = RemotePasswordBlacklistPolicyProviderFactory()
            .resolvePasswordBlacklist(webServer.url("/myBlacklist.txt").toString())

        assertNotNull(blacklist) { "There should be a blacklist" }
        assertTrue(blacklist?.contains("password") ?: false) { "The blacklist should contain the given words" }
        assertFalse(blacklist?.contains("awesome-password") ?: true) { "The blacklist should not contain words that are not given" }
    }

    @Test
    fun `successfully resolve a two password blacklists`() {
        val webServer = MockWebServer()
        webServer.start()
        webServer.enqueue(MockResponse.Builder()
            .code(200)
            .body("""
                password
                123456
            """.trimIndent())
            .build()
        )
        webServer.enqueue(MockResponse.Builder()
            .code(200)
            .body("""
                hidden
                unguessable
            """.trimIndent())
            .build()
        )

        val blacklist = RemotePasswordBlacklistPolicyProviderFactory()
            .resolvePasswordBlacklist("${webServer.url("/myBlacklist.txt")} ${webServer.url("/myOtherBlacklist.txt")}")

        assertNotNull(blacklist) { "There should be a blacklist" }
        assertTrue(blacklist?.contains("password") ?: false) { "The blacklist should contain the words from first blacklist" }
        assertTrue(blacklist?.contains("unguessable") ?: false) { "The blacklist should contain the words from the second blacklist" }
        assertFalse(blacklist?.contains("awesome-password") ?: true) { "The blacklist should not contain words that are not given" }
    }

    @Test
    fun `return null when the blacklist is not available`() {
        val webServer = MockWebServer()
        webServer.start()
        webServer.enqueue(MockResponse.Builder()
            .code(404)
            .build()
        )

        val blacklist = RemotePasswordBlacklistPolicyProviderFactory()
            .resolvePasswordBlacklist(webServer.url("/myBlacklist.txt").toString())

        assertNull(blacklist)
    }

    @Test
    fun `the blacklist reading should work case insensitive`() {
        val webServer = MockWebServer()
        webServer.start()
        webServer.enqueue(MockResponse.Builder()
            .code(200)
            .body("""
                PaSsWoRd
            """.trimIndent())
            .build()
        )

        val blacklist = RemotePasswordBlacklistPolicyProviderFactory()
            .resolvePasswordBlacklist(webServer.url("/myBlacklist.txt").toString())

        assertNotNull(blacklist) { "There should be a blacklist" }
        assertTrue(blacklist?.contains("pAsSwOrD") ?: false) { "The blacklist should contain the given case insensitive words" }
    }
}