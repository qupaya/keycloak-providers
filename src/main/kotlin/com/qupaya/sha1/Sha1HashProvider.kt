package com.qupaya.sha1

import org.keycloak.credential.hash.PasswordHashProvider
import org.keycloak.models.PasswordPolicy
import org.keycloak.models.credential.PasswordCredentialModel
import java.security.MessageDigest

/**
 * Verifies the salted SHA-1 hashes of passwords imported from a legacy system and never writes one.
 * Keycloak needs a provider for every algorithm still stored on a credential: a missing one makes
 * login fail for a current sha1 password and makes the password-history policy crash on a sha1
 * history entry. policyCheck rejects every credential, so a successful login re-hashes the
 * password with the realm's algorithm.
 */
class Sha1HashProvider : PasswordHashProvider {
    override fun policyCheck(policy: PasswordPolicy?, credential: PasswordCredentialModel?): Boolean = false

    override fun encodedCredential(rawPassword: String, iterations: Int): PasswordCredentialModel =
        throw UnsupportedOperationException("sha1 is verify-only; new passwords use the realm's hash algorithm")

    override fun verify(rawPassword: String, credential: PasswordCredentialModel): Boolean {
        val salt = credential.passwordSecretData.salt.decodeToString()
        val digest = MessageDigest.getInstance("SHA-1").digest((rawPassword + salt).toByteArray())
        val hex = digest.joinToString("") { "%02X".format(it) }
        return MessageDigest.isEqual(hex.toByteArray(), credential.passwordSecretData.value.toByteArray())
    }

    override fun close() {
    }

    companion object {
        const val ID = "sha1"
    }
}
