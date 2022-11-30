package com.qupaya.rest

import org.keycloak.models.KeycloakSession
import org.keycloak.services.resource.RealmResourceProvider

class RemoteBlacklistRealmResourceProvider(private val session: KeycloakSession?) : RealmResourceProvider {

    override fun getResource(): BlacklistReloadResource {
        return BlacklistReloadResource(this.session)
    }

    override fun close() {
        // noop
    }
}