package com.qupaya.rest

import org.keycloak.models.KeycloakSession
import org.keycloak.services.managers.AppAuthManager
import com.qupaya.RemotePasswordBlacklistPolicyProvider
import javax.ws.rs.ForbiddenException
import javax.ws.rs.GET
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

class BlacklistReloadResource(private val session: KeycloakSession?) {
    private val auth = AppAuthManager.BearerTokenAuthenticator(session).authenticate()

    @GET
    @Path("remote-blacklist")
    @Produces(MediaType.TEXT_PLAIN)
    fun triggerReload(): String {
        checkRealmAdmin()
        val blacklistProvider = this.session?.getProvider(RemotePasswordBlacklistPolicyProvider::class.java)
        // TODO implement reloading
        println("reloading ...")
        return "success"
    }

    private fun checkRealmAdmin() {
        if (auth == null) {
            throw NotAuthorizedException("Bearer")
        } else if (auth.token.realmAccess == null || !auth.token.realmAccess.isUserInRole("admin")) {
            throw ForbiddenException("Does not have realm admin role")
        }
    }
}