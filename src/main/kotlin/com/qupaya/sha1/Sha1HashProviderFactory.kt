package com.qupaya.sha1

import org.keycloak.Config
import org.keycloak.credential.hash.PasswordHashProvider
import org.keycloak.credential.hash.PasswordHashProviderFactory
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory

class Sha1HashProviderFactory : PasswordHashProviderFactory {
    private val provider = Sha1HashProvider()

    override fun create(session: KeycloakSession?): PasswordHashProvider = provider

    override fun init(config: Config.Scope?) {
    }

    override fun postInit(factory: KeycloakSessionFactory?) {
    }

    override fun getId(): String = Sha1HashProvider.ID

    override fun close() {
    }
}
