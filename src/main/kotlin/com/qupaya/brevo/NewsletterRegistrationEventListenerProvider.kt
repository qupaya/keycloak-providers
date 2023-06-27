package com.qupaya.brevo

import com.qupaya.util.urlEncode
import org.jboss.logging.Logger
import org.keycloak.events.Event
import org.keycloak.events.EventListenerProvider
import org.keycloak.events.EventType
import org.keycloak.events.admin.AdminEvent
import org.keycloak.models.KeycloakSession
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException


class NewsletterRegistrationEventListenerProvider(
    private val session: KeycloakSession,
    private val threadPool: ExecutorService,
) : EventListenerProvider {
    private val brevoFormLink = System.getenv("BREVO_FORM_LINK")

    init {
        if (brevoFormLink.isNullOrEmpty()) {
            LOG.warn("Environment variable BREVO_FORM_LINK is not set!")
        }
    }

    override fun onEvent(event: Event?) {
        if (event?.type != EventType.REGISTER) {
            return
        }

        val user = session.users().getUserById(session.context.realm, event.userId)
        if (user == null) {
            LOG.warn("Unable to find user with ID ${event.id}")
            return
        }

        val wantsNewsletter = user.attributes["newsletter"]
        if (wantsNewsletter == null || !wantsNewsletter.contains("on")) {
            return
        }

        val formData = mapOf(
            "EMAIL" to user.email,
            "OPT_IN" to "1",
            "email_address_check" to "",
            "locale" to "de",
            "html_type" to "simple",
        )

        try {
            threadPool.submit(createNewsletterSubscriptionRequestRunner(formData, event.userId))
        } catch (ex: RejectedExecutionException) {
            LOG.warn("The newsletter subscription requests is not queued. Maybe the system is shutting down.")
        }
    }

    private fun createNewsletterSubscriptionRequestRunner(formData: Map<String, String>, userId: String) = Runnable {
        /*
         * Yes, we could use the Brevo API to create a (DOI) contact, but
         * using the form data POST request ensures that handling like
         * success pages can be controlled inside Brevo.
         */
        val http = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(brevoFormLink))
            .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
            .header("content-type", "application/x-www-form-urlencoded")
            .build()

        for (waitTime in RETRY_WAIT_DURATIONS) {
            Thread.sleep(waitTime.toMillis())

            try {
                val response = http.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() < HTTP_ERROR_CODES_START) { // success
                    break
                }

                LOG.error("Brevo newsletter subscription request response: ${response.statusCode()}")
                LOG.error(response.body())
            } catch (ex: IOException) {
                LOG.error("IO exception while sending newsletter subscription request for user with ID $userId.", ex)
            } catch (ex: InterruptedException) {
                LOG.error("Interruption while sending newsletter subscription request for user with ID $userId.", ex)
            }
        }
    }

    override fun onEvent(event: AdminEvent?, includeRepresentation: Boolean) {
        // noop
    }

    override fun close() {
        // noop
    }

    companion object {
        private val LOG = Logger.getLogger(NewsletterRegistrationEventListenerProvider::class.java)
        private val RETRY_WAIT_DURATIONS = listOf(
            Duration.ZERO,
            Duration.ofSeconds(10),
            Duration.ofSeconds(30),
        )
        private const val HTTP_ERROR_CODES_START = 400

        private fun getFormDataAsString(formData: Map<String, String>): String {
            return formData.entries.joinToString("&") { (key, value) ->
                key.urlEncode() + "=" + value.urlEncode()
            }
        }
    }
}