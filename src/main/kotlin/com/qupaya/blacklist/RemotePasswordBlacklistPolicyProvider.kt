package com.qupaya.blacklist

import org.keycloak.models.KeycloakContext
import org.keycloak.models.RealmModel
import org.keycloak.models.UserModel
import org.keycloak.policy.PasswordPolicyProvider
import org.keycloak.policy.PolicyError

class RemotePasswordBlacklistPolicyProvider(private val resolver: BlacklistResolver, private val context: KeycloakContext?) : PasswordPolicyProvider {

    override fun validate(realm: RealmModel?, user: UserModel?, password: String?): PolicyError? {
        return validate(user?.username, password)
    }

    override fun validate(user: String?, password: String?): PolicyError? {
        if (password == null) {
            return null
        }

        val blacklist = this.context?.realm?.passwordPolicy?.getPolicyConfig<BlacklistResolver.PasswordBlacklist>(
            RemotePasswordBlacklistPolicyProviderFactory.ID
        )

        return if (blacklist?.contains(password) == true) {
            PolicyError(ERROR_MESSAGE)
        } else {
            null
        }
    }

    override fun parseConfig(addresses: String?): Any? {
        return addresses?.let { this.resolver.resolvePasswordBlacklist(it) }
    }

    override fun close() {
        // noop
    }

    companion object {
        const val ERROR_MESSAGE = "invalidPasswordBlacklistedMessage"
    }
}