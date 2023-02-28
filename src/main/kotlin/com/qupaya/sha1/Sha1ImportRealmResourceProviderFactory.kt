package com.qupaya.sha1

import org.keycloak.Config
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.services.resource.RealmResourceProvider
import org.keycloak.services.resource.RealmResourceProviderFactory

class Sha1ImportRealmResourceProviderFactory : RealmResourceProviderFactory {
    override fun create(session: KeycloakSession): RealmResourceProvider {
        return Sha1ImportRealmResourceProvider(session)
    }

    override fun init(config: Config.Scope?) {
        // noop
    }

    override fun postInit(factory: KeycloakSessionFactory?) {
        // noop
    }

    override fun close() {
        // noop
    }

    override fun getId(): String = ID

    companion object {
        const val ID = "sha1Import"
    }
}