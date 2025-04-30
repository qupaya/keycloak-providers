package com.qupaya.brevo

import com.qupaya.util.urlEncode
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.LaxRedirectStrategy
import org.apache.http.util.EntityUtils
import org.jboss.logging.Logger
import org.json.JSONObject
import org.keycloak.events.Event
import org.keycloak.events.EventListenerProvider
import org.keycloak.events.EventType
import org.keycloak.events.admin.AdminEvent
import org.keycloak.models.KeycloakSession
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException

class NewsletterRegistrationEventListenerProvider(
    private val session: KeycloakSession,
    private val threadPool: ExecutorService,
) : EventListenerProvider {
    private val brevoFormLink = System.getenv("BREVO_FORM_LINK")

    override fun onEvent(event: Event?) {
        if (event?.type != EventType.REGISTER) {
            return
        }

        val user = session.users().getUserById(session.context.realm, event.userId)

        LOG.info("User attributes: ${user.attributes}") // Log the full user attributes
        if (user == null) {
            LOG.warn("Unable to find user with ID ${event.userId}. No Brevo newsletter subscription will be done")
            return
        }

        if (brevoFormLink.isNullOrEmpty()) {
            LOG.warn("Environment variable BREVO_FORM_LINK is not set!")
            return
        } else {
            LOG.info("BREVO_FORM_LINK: $brevoFormLink")
        }

        val wantsNewsletter = user.attributes["newsletter"]
        if (wantsNewsletter == null || !wantsNewsletter.contains("on")) {
            LOG.info("Skipping Brevo newsletter subscription for ${user.id}")
            return
        }

        try {
            threadPool.submit(createNewsletterSubscriptionRequestRunner(brevoFormLink, user.email))
        } catch (ex: RejectedExecutionException) {
            LOG.warn("The Brevo newsletter subscription request is not queued. Maybe the system is shutting down.")
        }
    }

    private fun createNewsletterSubscriptionRequestRunner(url: String, email: String) = Runnable {
        try {
            val boundary = "----WebKitFormBoundaryCfnIdlFPwnlMHbbv" // Boundary from the web request

            // Create the multipart form data
            val entity = MultipartEntityBuilder.create()
                .setBoundary(boundary)
                .addTextBody("EMAIL", email)
                .addTextBody("OPT_IN", "1")
                .addTextBody("email_address_check", "")
                .addTextBody("locale", "de")
                .build()

            // Build the HttpPost request
            val httpPost = HttpPost(url)
            httpPost.entity = entity
            httpPost.setHeader("Content-Type", "multipart/form-data; boundary=$boundary")
            httpPost.setHeader("Accept", "*/*")
            // THIS IS IMPORTANT! Without this, the request "succeeds" but the server does not process it.
            httpPost.setHeader(
                "user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36"
            )


            LOG.info("Brevo newsletter subscription request for $email to $url")

            val httpClient = HttpClientBuilder.create()
                .setRetryHandler(DefaultHttpRequestRetryHandler())
                .setRedirectStrategy(LaxRedirectStrategy())
                .build()

            httpClient.use { client ->
                val response: CloseableHttpResponse = client.execute(httpPost)
                val statusCode = response.statusLine.statusCode
                val responseString = EntityUtils.toString(response.entity, Charset.forName("UTF-8")).trim()

                val contentType = response.getFirstHeader("Content-Type")?.value
                LOG.info("Content-Type: $contentType")

                // Log the response headers if you need it/want it
//                val responseHeaders = response.allHeaders
//                responseHeaders.forEach { header ->
//                    LOG.info("Response Header: ${header.name} = ${header.value}")
//                }

                // Log the response body for debugging
                LOG.info("Response Entity: $responseString")

                // Handle error status codes
                if (statusCode >= 400) {
                    LOG.error("Brevo newsletter subscription failed for $email: $statusCode")
                    LOG.error("Response: $responseString")

                } else {
                    // Parse the response as JSON
                    try {
                        val json = JSONObject(responseString)
                        val success = json.optBoolean("success", false)

                        if (success) {
                            LOG.info("Brevo newsletter subscription successful for $email")
                        } else {
                            LOG.error("Brevo newsletter subscription response for $email was not successful")
                        }
                    } catch (e: Exception) {
                        LOG.error("Error parsing response JSON: $responseString", e)
                    }
                }


            }
        } catch (ex: IOException) {
            LOG.error("IOException while sending Brevo newsletter subscription request for user with ID $email.", ex)
        } catch (ex: InterruptedException) {
            LOG.error("Interruption while sending Brevo newsletter subscription request for user with ID $email.", ex)
        } catch (ex: Exception) {
            LOG.error("Exception while sending Brevo newsletter subscription request for user with ID $email.", ex)
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
        private const val HTTP_ERROR_CODES_START = 400

        private fun getFormDataAsString(formData: Map<String, String>): String {
            return formData.entries.joinToString("&") { (key, value) ->
                key.urlEncode() + "=" + value.urlEncode()
            }
        }


    }
}
