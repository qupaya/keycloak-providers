package com.qupaya.blacklist

import com.qupaya.blacklist.rest.RemoteBlacklistResource
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.keycloak.models.KeycloakSession
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import javax.ws.rs.core.Response
import kotlin.test.assertEquals

class RemoteBlacklistResourceTest {

    private val blacklist = mock<BlacklistResolver.PasswordBlacklist> {
        on { contains(eq("password")) } doReturn true
    }
    private val context = createContext(blacklist)
    private val session = mock<KeycloakSession> {
        on { context } doReturn context
    }
    private val blacklistResource = RemoteBlacklistResource(session)

    @Test
    fun `find password in blacklist`() {
        val response = blacklistResource.checkPassword("password")

        assertEquals(Response.Status.CONFLICT, response.statusInfo)
    }

    @Test
    fun `password is not in blacklist`() {
        val response = blacklistResource.checkPassword("hello")

        assertEquals(Response.Status.OK, response.statusInfo)
    }

    @Test
    fun `the endpoint is annotated in the jakarta namespace Keycloak scans`() {
        val annotations = RemoteBlacklistResource::class.java
            .getDeclaredMethod("checkPassword", String::class.java)
            .annotations.map { it.annotationClass.java.name }

        assertTrue(annotations.none { it.startsWith("javax.ws.rs.") }) {
            "Keycloak 26 discovers endpoints through jakarta.ws.rs annotations, so javax.ws.rs ones are invisible to it. Found: $annotations"
        }
        assertTrue("jakarta.ws.rs.GET" in annotations) { "checkPassword must be reachable as a GET endpoint. Found: $annotations" }
        assertTrue("jakarta.ws.rs.Path" in annotations) { "checkPassword must declare its path. Found: $annotations" }
    }
}
