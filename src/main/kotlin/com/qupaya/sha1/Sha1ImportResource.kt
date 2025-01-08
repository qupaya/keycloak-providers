package com.qupaya.sha1

import org.keycloak.models.KeycloakSession
import org.keycloak.models.credential.PasswordCredentialModel
import org.keycloak.services.managers.AppAuthManager.BearerTokenAuthenticator
import java.nio.charset.StandardCharsets
import javax.ws.rs.*
import javax.ws.rs.core.CacheControl
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class Sha1ImportResource(private val session: KeycloakSession) {
    private val auth = BearerTokenAuthenticator(session).authenticate()

    @POST
    @Path("import")
    @Consumes(MediaType.APPLICATION_JSON)
    fun importUser(userData: Sha1User): Response {
        checkRealmAdmin()

        val cacheControl = CacheControl()
        cacheControl.isNoCache = true

        val existingUser = session.users().getUserByEmail(session.context.realm, userData.email)
        if (existingUser != null) {
            return Response.ok("{\"id\": \"${existingUser.id}\"}")
                .cacheControl(cacheControl)
                .build()
        }

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
            .cacheControl(cacheControl)
            .build()
    }

    private fun checkRealmAdmin() {
        if (auth == null) {
            throw NotAuthorizedException("Bearer")
        } else if (auth.token.realmAccess == null || !auth.token.realmAccess.isUserInRole("sha1-import")) {
            throw ForbiddenException("Does not have the required import role")
        }
    }
}