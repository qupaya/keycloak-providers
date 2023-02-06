package com.qupaya.rest

import org.keycloak.models.KeycloakSession
import org.keycloak.services.managers.AppAuthManager
import com.qupaya.RemotePasswordBlacklistPolicyProvider
import org.jboss.resteasy.annotations.cache.NoCache
import javax.ws.rs.ForbiddenException
import javax.ws.rs.GET
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class BlacklistReloadResource(private val session: KeycloakSession?) {
    private val auth = AppAuthManager.BearerTokenAuthenticator(session).authenticate()

    @GET
    @Path("remote-blacklist")
    @NoCache
    @Produces(MediaType.TEXT_PLAIN)
    fun triggerReload(): Response {
        checkRealmAdmin()
        val blacklistProvider = this.session?.getProvider(RemotePasswordBlacklistPolicyProvider::class.java)
        // TODO implement reloading
        println("reloading ...")
        return Response.ok("success").build()
    }

    private fun checkRealmAdmin() {
        println("auth: $auth, token: ${auth?.token}, access: ${auth?.token?.resourceAccess}, access: ${auth?.token?.realmAccess}, admin: ${auth?.token?.realmAccess?.isUserInRole("admin")}")
        if (auth == null) {
            throw NotAuthorizedException("Bearer")
        } else if (auth.token.realmAccess == null || !auth.token.realmAccess.isUserInRole("admin")) {
            throw ForbiddenException("Does not have realm admin role")
        }
    }
}