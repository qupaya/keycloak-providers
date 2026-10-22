package com.qupaya.emailalias

/**
 * The local part ends at the LAST `@`. Neither function throws; input without `@` has no sub-address.
 */
object CanonicalMailbox {
    private val GMAIL_DOMAINS = setOf("gmail.com", "googlemail.com")
    private const val GMAIL_CANONICAL_DOMAIN = "gmail.com"

    fun hasSubAddress(email: String): Boolean {
        val at = email.lastIndexOf('@')
        return at >= 0 && email.substring(0, at).contains('+')
    }

    /**
     * Lower-cased address with the `+tag` removed from the local part. For Gmail domains the dots are also
     * removed and `googlemail.com` becomes `gmail.com`. Input without `@` is only lower-cased.
     */
    fun canonicalMailbox(email: String): String {
        val lower = email.lowercase()
        val at = lower.lastIndexOf('@')
        if (at < 0) return lower

        val localWithoutTag = lower.substring(0, at).substringBefore('+')
        val domain = lower.substring(at + 1)
        if (domain !in GMAIL_DOMAINS) return "$localWithoutTag@$domain"

        return "${localWithoutTag.replace(".", "")}@$GMAIL_CANONICAL_DOMAIN"
    }
}
