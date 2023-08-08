package com.qupaya.brevo

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


class NewsletterRegistrationEventListenerProviderFactory : EventListenerProviderFactory,
    ServerInfoAwareProviderFactory {
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

    override fun getOperationalInfo(): Map<String, String> {
        val brevoFormLink = System.getenv("BREVO_FORM_LINK")
        var brevoRequestResult = "---"

        if (!brevoFormLink.isNullOrEmpty()) {
            val httpPost = HttpPost(brevoFormLink)
            httpPost.entity = StringEntity("")
            httpPost.setHeader("content-type", "application/x-www-form-urlencoded")

            try {
                HttpClientBuilder.create()
                    .setRetryHandler(DefaultHttpRequestRetryHandler())
                    .setRedirectStrategy(LaxRedirectStrategy()).build()
                    .use { http ->
                        val response = http.execute(httpPost)
                        if (response.statusLine.statusCode >= 400) {
                            LOG.error("Brevo newsletter subscription test request response: ${response.statusLine.statusCode}")
                            LOG.error(EntityUtils.toString(response.entity))
                            brevoRequestResult = "Brevo newsletter subscription test request response: ${response.statusLine.statusCode}"
                        }
                    }

            } catch (ex: IOException) {
                LOG.error(
                    "IO exception while sending newsletter subscription test request",
                    ex
                )
                brevoRequestResult = "IO exception while sending newsletter subscription test request"
            } catch (ex: InterruptedException) {
                LOG.error(
                    "Interruption while sending newsletter subscription test request.",
                    ex
                )
                brevoRequestResult = "Interruption while sending newsletter subscription test request."
            }
        }

        return mapOf(
            "brevoFormLink" to brevoFormLink,
            "testRequestResult" to brevoRequestResult,
        )
    }

    companion object {
        private const val ID = "brevo-newsletter-registration-event-listener"

        private val LOG = Logger.getLogger(NewsletterRegistrationEventListenerProviderFactory::class.java)
    }
}