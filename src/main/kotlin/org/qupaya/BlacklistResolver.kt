package org.qupaya

interface BlacklistResolver {
    interface PasswordBlacklist {
        fun contains(password: String): Boolean
    }

    fun resolvePasswordBlacklist(blacklistAddress: String): PasswordBlacklist?
}