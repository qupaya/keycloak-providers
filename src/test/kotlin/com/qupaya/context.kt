package com.qupaya

import org.keycloak.models.KeycloakContext
import org.keycloak.models.PasswordPolicy
import org.keycloak.models.RealmModel
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock


fun createContext(blacklist: BlacklistResolver.PasswordBlacklist?): KeycloakContext {
    val pwPolicy = mock<PasswordPolicy> {
        on { getPolicyConfig<BlacklistResolver.PasswordBlacklist>(eq(RemotePasswordBlacklistPolicyProviderFactory.ID)) } doReturn blacklist
    }
    val realmModel = mock<RealmModel> {
        on { passwordPolicy } doReturn pwPolicy
    }
    return mock {
        on { realm } doReturn realmModel
    }
}
