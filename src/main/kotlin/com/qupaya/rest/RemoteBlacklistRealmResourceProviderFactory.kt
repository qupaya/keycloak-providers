package com.qupaya.rest

import org.keycloak.Config
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.services.resource.RealmResourceProvider
import org.keycloak.services.resource.RealmResourceProviderFactory

class RemoteBlacklistRealmResourceProviderFactory : RealmResourceProviderFactory {
    override fun create(session: KeycloakSession?): RealmResourceProvider {
        return RemoteBlacklistRealmResourceProvider(session)
    }

    override fun init(config: Config.Scope?) {}

    override fun postInit(factory: KeycloakSessionFactory?) {}

    override fun close() {}

    override fun getId(): String = ID

    companion object {
        const val ID = "remoteBlacklistRealm"
    }
}