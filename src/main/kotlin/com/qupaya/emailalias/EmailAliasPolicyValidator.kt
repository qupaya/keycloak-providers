package com.qupaya.emailalias

import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.models.UserModel
import org.keycloak.userprofile.UserProfileAttributeValidationContext
import org.keycloak.validate.ValidationContext
import org.keycloak.validate.ValidationError
import org.keycloak.validate.Validator
import org.keycloak.validate.ValidatorConfig

/**
 * Rejects alias registrations for the user profile `email` attribute:
 * - Rule 1: a `+` in the local part (`emailSubAddressNotAllowed`).
 * - Rule 2: a Gmail dot variant of another existing user (`emailGmailMailboxTaken`, the existing spelling is
 *   message parameter `{1}`; `{0}` is the attribute name).
 *
 * Like Keycloak's `DuplicateEmailValidator`, nothing is checked when the edited user keeps their current email,
 * so existing `+` and dotted-Gmail users stay editable. At most one error is reported per run, Rule 1 first.
 * Messages are keys only; the text comes from the theme.
 */
class EmailAliasPolicyValidator : Validator {
    override fun validate(input: Any?, inputHint: String, context: ValidationContext, config: ValidatorConfig): ValidationContext {
        val value = (input as? List<*>)?.firstOrNull() as? String
        if (value.isNullOrBlank()) return context

        val editedUser = UserProfileAttributeValidationContext.from(context).attributeContext.user
        if (editedUser != null && value.equals(editedUser.getFirstAttribute(inputHint), ignoreCase = true)) return context

        if (CanonicalMailbox.hasSubAddress(value)) {
            context.addError(ValidationError(ID, inputHint, SUB_ADDRESS_MESSAGE))
            return context
        }

        val owner = findGmailMailboxOwner(context.session, value, editedUser?.id)
        if (owner != null) {
            context.addError(ValidationError(ID, inputHint, GMAIL_MAILBOX_TAKEN_MESSAGE, owner.email).setStatusCode(Response.Status.CONFLICT))
        }
        return context
    }

    /**
     * Searches with the `UserModel.SEARCH` key, whose term Keycloak lower-cases, escapes `\`, `%` and `_` in, and turns
     * `*` into `%`, appending `%` unless the term ends with `*` and anchoring the LIKE at the start of the email
     * (`JpaUserProvider`, 26.7.2). The `EMAIL` key is unusable: it wraps the raw term in `%...%` without
     * handling `*` or escaping LIKE characters. `SEARCH` also matches username and names, so the result is
     * only a candidate list that is re-checked against the canonical mailbox.
     */
    private fun findGmailMailboxOwner(session: KeycloakSession, value: String, editedUserId: String?): UserModel? {
        val domain = value.substringAfterLast('@').lowercase()
        if (domain !in GMAIL_DOMAINS) return null

        val canonical = CanonicalMailbox.canonicalMailbox(value)
        val canonicalLocalPart = canonical.substringBeforeLast('@').filterNot { it.isWhitespace() }
        if (canonicalLocalPart.isEmpty()) return null

        val realm = session.context.realm
        /*
          Letters in order with wildcards between them match every dot spelling, and the one after the last letter
          matches a stored `+tag`.
        */
        val localPattern = canonicalLocalPart.toList().joinToString(separator = "*", postfix = "*")

        for (gmailDomain in GMAIL_DOMAINS) {
            val owner = searchPages(session, realm, "$localPattern@$gmailDomain") {
                it.id != editedUserId && it.email != null && CanonicalMailbox.canonicalMailbox(it.email) == canonical
            }
            if (owner != null) return owner
        }
        return null
    }

    private fun searchPages(session: KeycloakSession, realm: RealmModel, searchTerm: String, isOwner: (UserModel) -> Boolean): UserModel? {
        val attributes = mapOf(
            UserModel.SEARCH to searchTerm,
            UserModel.INCLUDE_SERVICE_ACCOUNT to false.toString(),
        )
        for (page in 0 until MAX_PAGES) {
            val candidates = session.users().searchForUserStream(realm, attributes, page * PAGE_SIZE, PAGE_SIZE).use { stream ->
                stream.toList()
            }
            candidates.firstOrNull(isOwner)?.let { return it }
            if (candidates.size < PAGE_SIZE) return null
        }
        /* Fail open: refusing every registration because of a crowded prefix would be worse than missing one alias. */
        LOG.warnf("Gmail mailbox lookup hit the cap of %d candidates without a match; allowing the email", MAX_PAGES * PAGE_SIZE)
        return null
    }

    companion object {
        const val ID = "email-alias-policy"
        const val SUB_ADDRESS_MESSAGE = "emailSubAddressNotAllowed"
        const val GMAIL_MAILBOX_TAKEN_MESSAGE = "emailGmailMailboxTaken"
        private const val PAGE_SIZE = 50
        private const val MAX_PAGES = 10
        private val GMAIL_DOMAINS = listOf("gmail.com", "googlemail.com")
        private val LOG = Logger.getLogger(EmailAliasPolicyValidator::class.java)
    }
}
