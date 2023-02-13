package com.qupaya

import com.google.common.hash.BloomFilter
import com.google.common.hash.Funnels
import org.jboss.logging.Logger
import org.keycloak.Config
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.policy.BlacklistPasswordPolicyProviderFactory
import org.keycloak.policy.PasswordPolicyProvider
import org.keycloak.policy.PasswordPolicyProviderFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class RemotePasswordBlacklistPolicyProviderFactory : PasswordPolicyProviderFactory, BlacklistResolver {
    private val blacklistRegistry: ConcurrentMap<String, UrlBasedPasswordBlacklist> = ConcurrentHashMap()

    override fun create(session: KeycloakSession?): PasswordPolicyProvider {
        return RemotePasswordBlacklistPolicyProvider(this, session?.context)
    }

    override fun init(config: Config.Scope?) {
    }

    override fun postInit(factory: KeycloakSessionFactory?) {
    }

    override fun getId(): String = ID

    override fun getDisplayName(): String = "Remote Password Blacklist"

    override fun getConfigType(): String = PasswordPolicyProvider.STRING_CONFIG_TYPE

    override fun getDefaultConfigValue(): String = ""

    override fun isMultiplSupported(): Boolean = false

    override fun close() {
    }

    /**
     * Resolves and potentially registers a [BlacklistPasswordPolicyProviderFactory.PasswordBlacklist] for the given `blacklistName`.
     *
     * @param blacklistAddresses HTTP address of the password blacklist
     * @return
     */
    override fun resolvePasswordBlacklist(blacklistAddresses: String): BlacklistResolver.PasswordBlacklist? {
        Objects.requireNonNull(blacklistAddresses, "blacklistName")
        val cleanedBlacklistAddresses = blacklistAddresses.trim()

        require(cleanedBlacklistAddresses.isNotEmpty()) { "Password blacklist name must not be empty!" }

        blacklistRegistry.clear()
        return blacklistRegistry.computeIfAbsent(
            cleanedBlacklistAddresses
        ) { addresses: String ->
            val pbl = UrlBasedPasswordBlacklist(addresses)
            try {
                pbl.init()
                pbl
            } catch (ex: Exception) {
                LOG.error("Unable to create password blacklist", ex)
                null
            }
        }
    }

    inner class UrlBasedPasswordBlacklist(private val addresses: String) : BlacklistResolver.PasswordBlacklist {
        private var blacklist: BloomFilter<String>? = null

        init {
            if (!this.addresses.split(' ').any { it.matches(Regex("^https?://.+")) }) {
                throw IllegalArgumentException("One of the addresses $addresses is not an HTTP address!")
            }
        }

        fun init() {
            if (this.blacklist != null) {
                return
            }

            LOG.info("loading blacklists from ${this.addresses}")

            this.blacklist = this.addresses.split(' ')
                .map { address ->
                    this.loadPasswordBlacklist(address)
                        .split('\n')
                }
                .flatten()
                .toSet()
                .let { loadBloomFilter(it) }

            LOG.info("successfully loaded blacklists from ${this.addresses}")
        }

        private fun loadPasswordBlacklist(address: String): String {
            try {
                val request = HttpRequest.newBuilder(URI(address))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build()

                val response = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build()
                    .send(request, BodyHandlers.ofString())

                if (response.statusCode() >= 300) {
                    throw RuntimeException("Request returned with status code ${response.statusCode()}")
                }

                return response.body()
            } catch (ex: Exception) {
                throw RuntimeException("Could not load password blacklist from addresses $addresses", ex)
            }
        }

        private fun loadBloomFilter(blacklist: Set<String>): BloomFilter<String> {
            val filter: BloomFilter<String> = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                blacklist.size,
                FALSE_POSITIVE_PROBABILITY
            )

            blacklist.forEach(filter::put)

            return filter
        }

        override fun contains(password: String): Boolean {
            return this.blacklist?.mightContain(password) ?: true
        }
    }

    companion object {
        const val ID = "remotePasswordBlacklist"
        const val FALSE_POSITIVE_PROBABILITY = 0.01
        val LOG = Logger.getLogger(RemotePasswordBlacklistPolicyProviderFactory::class.java)
    }
}