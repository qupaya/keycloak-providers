package com.qupaya

import com.qupaya.rest.RemoteBlacklistResource
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
}