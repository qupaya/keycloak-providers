package com.qupaya.emailalias

import jakarta.ws.rs.core.Response
import org.keycloak.models.KeycloakContext
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.models.UserModel
import org.keycloak.models.UserProvider
import org.keycloak.userprofile.AttributeContext
import org.keycloak.userprofile.UserProfileAttributeValidationContext
import org.keycloak.userprofile.UserProfileContext
import org.keycloak.validate.ValidationContext
import org.keycloak.validate.ValidationError
import org.keycloak.validate.ValidatorConfig
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class EmailAliasPolicyValidatorTest {
    private data class StoredUser(val id: String, val email: String)

    private val realm = mock<RealmModel>()
    private val storedUsers = mutableListOf<StoredUser>()
    private val userProvider = mock<UserProvider> {
        /* Fakes the SEARCH-key semantics described on EmailAliasPolicyValidator.findGmailMailboxOwner. */
        on { searchForUserStream(eq(realm), any<Map<String, String>>(), anyOrNull(), anyOrNull()) } doAnswer { call ->
            val attributes = call.getArgument<Map<String, String>>(1)
            val first = call.getArgument<Int>(2)
            val max = call.getArgument<Int>(3)
            val term = attributes.getValue(UserModel.SEARCH).lowercase()
            val like = Regex(term.split("*").joinToString(".*") { Regex.escape(it) } + if (term.endsWith("*")) "" else ".*")
            storedUsers.filter { like.matches(it.email.lowercase()) }
                .drop(first)
                .take(max)
                .map { stored ->
                    val candidate = mock<UserModel>()
                    whenever(candidate.id).thenReturn(stored.id)
                    whenever(candidate.email).thenReturn(stored.email)
                    candidate
                }
                .stream()
        }
    }
    private val keycloakContext = mock<KeycloakContext>()
    private val session = mock<KeycloakSession>()
    private val validator = EmailAliasPolicyValidator()

    init {
        whenever(keycloakContext.realm).thenReturn(realm)
        whenever(session.context).thenReturn(keycloakContext)
        whenever(session.users()).thenReturn(userProvider)
    }

    private fun existingUser(id: String, email: String): UserModel {
        val user = mock<UserModel>()
        whenever(user.id).thenReturn(id)
        whenever(user.getFirstAttribute("email")).thenReturn(email)
        return user
    }

    private fun validate(input: Any?, editedUser: UserModel? = null): Set<ValidationError> {
        val attribute = mapOf("email" to ((input as? List<*>)?.filterIsInstance<String>() ?: emptyList())).entries.first()
        val attributeContext = AttributeContext(UserProfileContext.USER_API, session, attribute, editedUser, mock(), mock())
        val context: ValidationContext = UserProfileAttributeValidationContext(attributeContext)
        return validator.validate(input, "email", context, ValidatorConfig.EMPTY).errors
    }

    @Test
    fun `rejects a plus in the local part of a new user`() {
        val errors = validate(listOf("anna+shop@web.de"))

        val error = errors.single()
        assertEquals("emailSubAddressNotAllowed", error.message)
        assertEquals("email-alias-policy", error.validatorId)
        assertEquals(listOf<Any>("email"), error.inputHintWithMessageParameters.toList())
    }

    @Test
    fun `rejects a gmail dot variant of an existing user and names the existing spelling`() {
        storedUsers += StoredUser("u1", "anna@gmail.com")

        val error = validate(listOf("a.nna@gmail.com")).single()

        assertEquals("emailGmailMailboxTaken", error.message)
        assertEquals(listOf<Any>("email", "anna@gmail.com"), error.inputHintWithMessageParameters.toList())
        assertEquals(Response.Status.CONFLICT, error.statusCode)
    }

    @Test
    fun `rejects a dotted spelling of a stored dotted gmail address`() {
        storedUsers += StoredUser("u1", "a.n.n.a@gmail.com")

        val error = validate(listOf("an.na@gmail.com")).single()

        assertEquals(listOf<Any>("email", "a.n.n.a@gmail.com"), error.inputHintWithMessageParameters.toList())
    }

    @Test
    fun `rejects a googlemail variant of a stored gmail address`() {
        storedUsers += StoredUser("u1", "anna@gmail.com")

        assertEquals("emailGmailMailboxTaken", validate(listOf("a.nna@googlemail.com")).single().message)
    }

    @Test
    fun `rejects a gmail variant of a stored googlemail address`() {
        storedUsers += StoredUser("u1", "anna@googlemail.com")

        assertEquals("emailGmailMailboxTaken", validate(listOf("a.nna@gmail.com")).single().message)
    }

    @Test
    fun `rejects a dot variant of a stored plus-tagged gmail address`() {
        storedUsers += StoredUser("u1", "anna+shop@gmail.com")

        val error = validate(listOf("a.nna@gmail.com")).single()

        assertEquals(listOf<Any>("email", "anna+shop@gmail.com"), error.inputHintWithMessageParameters.toList())
    }

    @Test
    fun `finds the existing user when the stored email has other casing`() {
        storedUsers += StoredUser("u1", "Anna@Gmail.com")

        assertEquals("emailGmailMailboxTaken", validate(listOf("A.nna@gmail.com")).single().message)
    }

    @Test
    fun `allows a gmail address that is not a variant of any existing user`() {
        storedUsers += StoredUser("u1", "anna@gmail.com")
        storedUsers += StoredUser("u2", "annabel@gmail.com")
        storedUsers += StoredUser("u3", "banna@gmail.com")

        assertTrue(validate(listOf("an.na.x@gmail.com")).isEmpty())
    }

    @Test
    fun `an existing user keeping an unchanged plus email passes, also with different case`() {
        val user = existingUser("u1", "Anna+Shop@Web.de")

        assertTrue(validate(listOf("Anna+Shop@Web.de"), user).isEmpty())
        assertTrue(validate(listOf("anna+shop@web.de"), user).isEmpty())
    }

    @Test
    fun `an unchanged email skips the lookup`() {
        val user = existingUser("u1", "a.nna@gmail.com")
        storedUsers += StoredUser("u2", "anna@gmail.com")

        assertTrue(validate(listOf("A.nna@gmail.com"), user).isEmpty())
        verify(userProvider, never()).searchForUserStream(any<RealmModel>(), any<Map<String, String>>(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `an existing user changing to their own dot variant is allowed`() {
        storedUsers += StoredUser("u1", "anna@gmail.com")
        val user = existingUser("u1", "anna@gmail.com")

        assertTrue(validate(listOf("a.nna@gmail.com"), user).isEmpty())
    }

    @Test
    fun `an existing user changing to another users dot variant is rejected`() {
        storedUsers += StoredUser("u1", "anna@gmail.com")
        storedUsers += StoredUser("u2", "bert@gmail.com")
        val user = existingUser("u2", "bert@gmail.com")

        val error = validate(listOf("a.nna@gmail.com"), user).single()

        assertEquals("emailGmailMailboxTaken", error.message)
    }

    @Test
    fun `an existing user changing to a plus address is rejected`() {
        val user = existingUser("u1", "anna@web.de")

        assertEquals("emailSubAddressNotAllowed", validate(listOf("anna+x@web.de"), user).single().message)
    }

    @Test
    fun `dots in non-gmail addresses are allowed`() {
        storedUsers += StoredUser("u1", "anna@web.de")

        assertTrue(validate(listOf("a.nna@web.de")).isEmpty())
        verify(userProvider, never()).searchForUserStream(any<RealmModel>(), any<Map<String, String>>(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `blank empty and null input produce no error`() {
        assertTrue(validate(listOf("")).isEmpty())
        assertTrue(validate(listOf("   ")).isEmpty())
        assertTrue(validate(emptyList<String>()).isEmpty())
        assertTrue(validate(null).isEmpty())
    }

    @Test
    fun `reports only the sub-address error when both rules are violated`() {
        storedUsers += StoredUser("u1", "anna@gmail.com")

        val error = validate(listOf("a.nna+x@gmail.com")).single()

        assertEquals("emailSubAddressNotAllowed", error.message)
    }

    @Test
    fun `searches with a bounded wildcard pattern per gmail domain`() {
        validate(listOf("A.n.Na@Gmail.com"))

        val attributes = argumentCaptor<Map<String, String>>()
        val max = argumentCaptor<Int>()
        verify(userProvider, atLeast(2)).searchForUserStream(eq(realm), attributes.capture(), any(), max.capture())
        assertEquals(setOf("a*n*n*a*@gmail.com", "a*n*n*a*@googlemail.com"), attributes.allValues.map { it.getValue(UserModel.SEARCH) }.toSet())
        assertTrue(attributes.allValues.all { it[UserModel.INCLUDE_SERVICE_ACCOUNT] == "false" })
        assertTrue(max.allValues.all { it in 1..500 })
    }

    @Test
    fun `LIKE metacharacters in the local part are only used in the escaped search key`() {
        storedUsers += StoredUser("u1", "a_b@gmail.com")
        storedUsers += StoredUser("u2", "axb@gmail.com")

        assertTrue(validate(listOf("a%b@gmail.com")).isEmpty())
        assertEquals("emailGmailMailboxTaken", validate(listOf("a_.b@gmail.com")).single().message)
    }

    @Test
    fun `finds a match beyond the first page of candidates`() {
        repeat(120) { storedUsers += StoredUser("n$it", "a-$it-n@gmail.com") }
        storedUsers += StoredUser("target", "an@gmail.com")

        val error = validate(listOf("a.n@gmail.com")).single()

        assertEquals(listOf<Any>("email", "an@gmail.com"), error.inputHintWithMessageParameters.toList())
    }

    @Test
    fun `fails open when the candidate cap is reached without a match`() {
        repeat(2000) { storedUsers += StoredUser("n$it", "a-$it-n@gmail.com") }
        storedUsers += StoredUser("target", "an@gmail.com")

        assertTrue(validate(listOf("a.n@gmail.com")).isEmpty())
    }

    @Test
    fun `the factory exposes the validator under its id`() {
        val factory = EmailAliasPolicyValidatorFactory()

        assertEquals("email-alias-policy", factory.id)
        assertNotNull(factory.create(session))
    }
}
