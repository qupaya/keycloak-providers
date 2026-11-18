package com.qupaya.sha1

import org.keycloak.models.credential.PasswordCredentialModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Sha1HashProviderTest {
    private val provider = Sha1HashProviderFactory().create(null)

    @Test
    fun `verify accepts the password whose salted SHA-1 is stored`() {
        assertTrue(provider.verify(PASSWORD, importedCredential()))
    }

    @Test
    fun `verify rejects a different password`() {
        assertFalse(provider.verify("Legacy-Pass-2", importedCredential()))
    }

    @Test
    fun `refuses to encode a new password as sha1`() {
        assertFailsWith<UnsupportedOperationException> { provider.encodedCredential(PASSWORD, 1) }
    }

    @Test
    fun `reports every sha1 credential as outdated so Keycloak re-hashes it`() {
        assertFalse(provider.policyCheck(null, importedCredential()))
    }

    /* The id is persisted as the algorithm of every imported credential, so the literal is the contract. */
    @Test
    fun `registers under the algorithm id stored on imported credentials`() {
        assertEquals("sha1", Sha1HashProviderFactory().id)
    }

    private fun importedCredential(): PasswordCredentialModel =
        PasswordCredentialModel.createFromValues("sha1", SALT.toByteArray(), 1, STORED_HASH)

    private companion object {
        const val PASSWORD = "Legacy-Pass-1"
        const val SALT = "Xq7salt"

        /* Computed outside the code under test: printf '%s' 'Legacy-Pass-1Xq7salt' | shasum -a 1, uppercased. */
        const val STORED_HASH = "8E157E4DB556273E91D311109ED4B7B017EA3B66"
    }
}
