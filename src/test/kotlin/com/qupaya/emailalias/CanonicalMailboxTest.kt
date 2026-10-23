package com.qupaya.emailalias

import org.json.JSONArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalMailboxTest {
    private data class Vector(val input: String, val hasSubAddress: Boolean, val canonicalMailbox: String)

    private val vectors: List<Vector> = run {
        val json = checkNotNull(javaClass.getResourceAsStream("/email-alias/canonical-mailbox-vectors.json")) {
            "canonical-mailbox-vectors.json missing from test resources"
        }.use { it.readBytes().toString(Charsets.UTF_8) }
        val array = JSONArray(json)
        (0 until array.length()).map {
            val entry = array.getJSONObject(it)
            Vector(entry.getString("input"), entry.getBoolean("hasSubAddress"), entry.getString("canonicalMailbox"))
        }
    }

    @Test
    fun `vector file is not empty`() {
        assertTrue(vectors.isNotEmpty())
    }

    @Test
    fun `hasSubAddress matches every shared vector`() {
        vectors.forEach {
            assertEquals(it.hasSubAddress, CanonicalMailbox.hasSubAddress(it.input), "hasSubAddress(\"${it.input}\")")
        }
    }

    @Test
    fun `canonicalMailbox matches every shared vector`() {
        vectors.forEach {
            assertEquals(it.canonicalMailbox, CanonicalMailbox.canonicalMailbox(it.input), "canonicalMailbox(\"${it.input}\")")
        }
    }
}
