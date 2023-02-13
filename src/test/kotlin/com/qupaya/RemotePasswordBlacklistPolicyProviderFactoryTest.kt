package com.qupaya

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class RemotePasswordBlacklistPolicyProviderFactoryTest {

    @Test
    fun `successfully resolve a password blacklist`() {
        val webServer = MockWebServer()
        webServer.enqueue(MockResponse()
            .setBody("""
                password
                123456
            """.trimIndent())
            .setResponseCode(200)
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
        webServer.enqueue(MockResponse()
            .setBody("""
                password
                123456
            """.trimIndent())
            .setResponseCode(200)
        )
        webServer.enqueue(MockResponse()
            .setBody("""
                hidden
                unguessable
            """.trimIndent())
            .setResponseCode(200)
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
        webServer.enqueue(MockResponse()
            .setResponseCode(404)
        )

        val blacklist = RemotePasswordBlacklistPolicyProviderFactory()
            .resolvePasswordBlacklist(webServer.url("/myBlacklist.txt").toString())

        assertNull(blacklist)
    }
}