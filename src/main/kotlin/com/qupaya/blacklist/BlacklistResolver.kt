package com.qupaya.blacklist

interface BlacklistResolver {
    interface PasswordBlacklist {
        fun contains(password: String): Boolean
    }

    fun resolvePasswordBlacklist(blacklistAddresses: String): PasswordBlacklist?
}