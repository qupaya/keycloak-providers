package com.qupaya.emailalias

import org.keycloak.Config
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.provider.ConfiguredProvider
import org.keycloak.provider.ProviderConfigProperty
import org.keycloak.validate.Validator
import org.keycloak.validate.ValidatorFactory

class EmailAliasPolicyValidatorFactory : ValidatorFactory, ConfiguredProvider {
    private val validator = EmailAliasPolicyValidator()

    override fun create(session: KeycloakSession?): Validator = validator

    override fun init(config: Config.Scope?) {
    }

    override fun postInit(factory: KeycloakSessionFactory?) {
    }

    override fun getId(): String = EmailAliasPolicyValidator.ID

    override fun getHelpText(): String =
        "Rejects email addresses with a sub-address (a '+' in the local part) and Gmail dot variants of an existing user's address."

    override fun getConfigProperties(): List<ProviderConfigProperty> = emptyList()

    override fun close() {
    }
}
