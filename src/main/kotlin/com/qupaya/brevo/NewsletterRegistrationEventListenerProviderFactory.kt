package com.qupaya.brevo

import org.keycloak.Config
import org.keycloak.events.EventListenerProvider
import org.keycloak.events.EventListenerProviderFactory
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory


class NewsletterRegistrationEventListenerProviderFactory : EventListenerProviderFactory {
    override fun create(session: KeycloakSession): EventListenerProvider {
        return NewsletterRegistrationEventListenerProvider(session)
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
        private const val ID = "brevo-newsletter-registration-event-listener"
    }
}