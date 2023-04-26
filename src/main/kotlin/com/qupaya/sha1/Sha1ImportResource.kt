package com.qupaya.sha1

import org.jboss.resteasy.annotations.cache.NoCache
import org.keycloak.models.KeycloakSession
import org.keycloak.models.credential.PasswordCredentialModel
import org.keycloak.services.managers.AppAuthManager.BearerTokenAuthenticator
import java.nio.charset.StandardCharsets
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class Sha1ImportResource(private val session: KeycloakSession) {
    private val auth = BearerTokenAuthenticator(session).authenticate()

    @POST
    @Path("import")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    fun importUser(userData: Sha1User): Response {
        checkRealmAdmin()

        val user = session.users().addUser(session.context.realm, userData.username)
        user.firstName = userData.firstName
        user.lastName = userData.lastName
        user.email = userData.email
        user.isEnabled = userData.enabled
        user.isEmailVerified = userData.emailVerified

        user.credentialManager().createStoredCredential(
            PasswordCredentialModel.createFromValues(
                "sha1",
                userData.salt.toByteArray(StandardCharsets.UTF_8),
                1,
                userData.hash
            )
        )

        return Response.status(Response.Status.CREATED)
            .entity("{\"id\": \"${user.id}\"}")
            .build()
    }

    private fun checkRealmAdmin() {
        if (auth == null) {
            throw NotAuthorizedException("Bearer")
        } else if (auth.token.realmAccess == null || !auth.token.realmAccess.isUserInRole("sha1-import")) {
            println(auth.token.realmAccess?.roles)
            throw ForbiddenException("Does not have the required import role")
        }
    }
}