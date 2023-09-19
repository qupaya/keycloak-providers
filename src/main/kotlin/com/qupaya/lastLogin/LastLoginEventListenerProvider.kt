package com.qupaya.lastLogin

import org.jboss.logging.Logger
import org.keycloak.events.Event
import org.keycloak.events.EventListenerProvider
import org.keycloak.events.EventType
import org.keycloak.events.admin.AdminEvent
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmProvider
import org.keycloak.models.UserModel
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class LastLoginEventListenerProvider(
    private val session: KeycloakSession,
) : EventListenerProvider {
    private val log = Logger.getLogger(LastLoginEventListenerProvider::class.java)
    private val model: RealmProvider? = session.realms()


    override fun onEvent(event: Event) {
        // log.infof("## NEW %s EVENT", event.getType());
        if (EventType.LOGIN == event.type) {
            val realm = model!!.getRealm(event.realmId)
            val user: UserModel = this.session.users().getUserById(realm, event.userId)

            log.info("Updating last login status for user: " + event.userId)

            // Use current server time for login event
            val loginTime = OffsetDateTime.now(ZoneOffset.UTC)
            val loginTimeS = DateTimeFormatter.ISO_DATE_TIME.format(loginTime)
            user.setSingleAttribute("last-login", loginTimeS)
        }
    }

    override fun onEvent(adminEvent: AdminEvent?, b: Boolean) {}

    override fun close() {
        // Nothing to close
    }
}
