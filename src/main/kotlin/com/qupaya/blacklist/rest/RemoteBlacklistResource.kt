package com.qupaya.blacklist.rest

import com.qupaya.blacklist.BlacklistResolver
import com.qupaya.blacklist.RemotePasswordBlacklistPolicyProviderFactory
import org.keycloak.models.KeycloakSession
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.CacheControl
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class RemoteBlacklistResource(private val session: KeycloakSession) {

    @GET
    @Path("check/{password}")
    @Produces(MediaType.TEXT_PLAIN)
    fun checkPassword(@PathParam("password") password: String): Response {
        val blacklist = this.session.context?.realm?.passwordPolicy?.getPolicyConfig<BlacklistResolver.PasswordBlacklist>(
            RemotePasswordBlacklistPolicyProviderFactory.ID
        )

        val cacheControl = CacheControl()
        cacheControl.isNoCache = true

        if (blacklist?.contains(password) == true) {
            return Response
                .status(Response.Status.CONFLICT).entity("password is blacklisted")
                .cacheControl(cacheControl)
                .build()
        }
        return Response
            .ok("password is not blacklisted")
            .cacheControl(cacheControl)
            .build()
    }
}