package com.qupaya.lastLogin

import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.LaxRedirectStrategy
import org.apache.http.util.EntityUtils
import org.jboss.logging.Logger
import org.keycloak.Config
import org.keycloak.events.EventListenerProvider
import org.keycloak.events.EventListenerProviderFactory
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.provider.ServerInfoAwareProviderFactory
import java.io.IOException
import java.util.concurrent.Executors


class LastLoginEventListenerProviderFactory : EventListenerProviderFactory{


    override fun create(session: KeycloakSession): EventListenerProvider {
        return LastLoginEventListenerProvider(session)
    }

    override fun init(config: Config.Scope?) {
        // noop
    }

    override fun postInit(factory: KeycloakSessionFactory?) {
        // noop
    }

    override fun close() {
    }

    override fun getId(): String = "qupaya-lastLogin-event-listener"

    companion object {
        private val LOG = Logger.getLogger(LastLoginEventListenerProviderFactory::class.java)
    }
}