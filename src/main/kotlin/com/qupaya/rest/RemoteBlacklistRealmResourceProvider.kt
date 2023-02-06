package com.qupaya.rest

import org.keycloak.models.KeycloakSession
import org.keycloak.services.resource.RealmResourceProvider

class RemoteBlacklistRealmResourceProvider(private val session: KeycloakSession?) : RealmResourceProvider {

    override fun getResource(): Any {
        return BlacklistReloadResource(this.session)
    }

    override fun close() {
        // noop
    }
}