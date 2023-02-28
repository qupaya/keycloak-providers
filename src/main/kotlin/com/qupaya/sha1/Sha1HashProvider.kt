package com.qupaya.sha1

import org.apache.commons.codec.digest.DigestUtils
import org.keycloak.Config
import org.keycloak.credential.hash.PasswordHashProvider
import org.keycloak.credential.hash.PasswordHashProviderFactory
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.models.PasswordPolicy
import org.keycloak.models.credential.PasswordCredentialModel
import org.mindrot.jbcrypt.BCrypt
import java.math.BigInteger
import java.security.MessageDigest

class Sha1HashProvider : PasswordHashProviderFactory, PasswordHashProvider {
    override fun create(session: KeycloakSession?): PasswordHashProvider = this

    override fun init(config: Config.Scope?) {
        // noop
    }

    override fun postInit(factory: KeycloakSessionFactory?) {
        // noop
    }

    override fun close() {
        // noop
    }

    override fun policyCheck(policy: PasswordPolicy?, credential: PasswordCredentialModel?): Boolean {
        return credential?.passwordCredentialData?.hashIterations == 1 && ID == credential.passwordCredentialData.algorithm
    }

    override fun encodedCredential(rawPassword: String, iterations: Int): PasswordCredentialModel {
        val salt = BCrypt.gensalt()
        val encodedPassword = try {
            val hexPrivateCreditional: String = DigestUtils.sha1Hex(rawPassword.toByteArray())
            BCrypt.hashpw(hexPrivateCreditional, salt).uppercase()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e)
        }

        return PasswordCredentialModel.createFromValues(
            ID,
            salt.toByteArray(),
            1,
            encodedPassword
        )
    }

    override fun verify(rawPassword: String, credential: PasswordCredentialModel): Boolean {
        val encodedPassword = try {
            val digester = MessageDigest.getInstance("SHA-1")
            val saltedPassword = (rawPassword + credential.passwordSecretData.salt.decodeToString()).toByteArray()
            val hash = digester.digest(saltedPassword)
            String.format("%040x", BigInteger(1, hash)).uppercase()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
        return encodedPassword == credential.passwordSecretData.value
    }

    override fun getId(): String = ID

    companion object {
        const val ID = "sha1"
    }
}