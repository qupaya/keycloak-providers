package com.qupaya.sha1

import org.keycloak.models.KeycloakSession
import org.keycloak.services.resource.RealmResourceProvider

class Sha1ImportRealmResourceProvider(private val session: KeycloakSession) : RealmResourceProvider {
    override fun getResource(): Sha1ImportResource = Sha1ImportResource(session)

    override fun close() {
        // noop
    }
}
