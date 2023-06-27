package com.qupaya.brevo

import org.jboss.logging.Logger
import org.keycloak.Config
import org.keycloak.events.EventListenerProvider
import org.keycloak.events.EventListenerProviderFactory
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import java.util.concurrent.Executors


class NewsletterRegistrationEventListenerProviderFactory : EventListenerProviderFactory {
    private val threadPool = Executors.newFixedThreadPool(1)

    override fun create(session: KeycloakSession): EventListenerProvider {
        return NewsletterRegistrationEventListenerProvider(session, threadPool)
    }

    override fun init(config: Config.Scope?) {
        // noop
    }

    override fun postInit(factory: KeycloakSessionFactory?) {
        // noop
    }

    override fun close() {
        threadPool.shutdown()
    }

    override fun getId(): String = ID

    companion object {
        private const val ID = "brevo-newsletter-registration-event-listener"

        private val LOG = Logger.getLogger(NewsletterRegistrationEventListenerProviderFactory::class.java)
    }
}