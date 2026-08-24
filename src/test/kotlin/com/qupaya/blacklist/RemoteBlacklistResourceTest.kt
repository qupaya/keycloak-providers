package com.qupaya.blacklist

import com.qupaya.blacklist.rest.RemoteBlacklistResource
import org.keycloak.models.KeycloakSession
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        val method = RemoteBlacklistResource::class.java.getDeclaredMethod("checkPassword", String::class.java)
        val onClass = RemoteBlacklistResource::class.java.annotations.map { it.annotationClass.java.name }
        val onMethod = method.annotations.map { it.annotationClass.java.name }
        val onParameters = method.parameterAnnotations.flatMap { it.map { a -> a.annotationClass.java.name } }
        val all = onClass + onMethod + onParameters

        assertTrue(
            all.none { it.startsWith("javax.ws.rs.") },
            "Keycloak 26 discovers endpoints through jakarta.ws.rs annotations, so javax.ws.rs ones are invisible to it. Found: $all",
        )
        assertTrue("jakarta.ws.rs.GET" in onMethod, "checkPassword must be reachable as a GET endpoint. Found: $onMethod")
        assertTrue(
            "jakarta.ws.rs.PathParam" in onParameters,
            "The password parameter must be bound with jakarta.ws.rs.PathParam. A javax PathParam next to a jakarta @GET leaves the endpoint unroutable. Found: $onParameters",
        )
        assertEquals(
            "check/{password}",
            method.getAnnotation(Path::class.java)?.value,
            "The path the README publishes is part of the contract",
        )
    }
}
