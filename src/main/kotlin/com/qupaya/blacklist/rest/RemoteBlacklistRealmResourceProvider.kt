package com.qupaya.blacklist.rest

import org.keycloak.models.KeycloakSession
import org.keycloak.services.resource.RealmResourceProvider

class RemoteBlacklistRealmResourceProvider(private val session: KeycloakSession) : RealmResourceProvider {
    override fun getResource(): RemoteBlacklistResource {
        return RemoteBlacklistResource(this.session)
    }

    override fun close() {
        // noop
    }
}