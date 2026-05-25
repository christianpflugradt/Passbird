package de.pflugradts.passbird.application.passwordtree

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse

class PasswordTreeEnvelopeTest {
    private val passwordTreeEnvelope = PasswordTreeEnvelope()
    private val payload = "payload".toByteArray()

    @Test
    fun `should unwrap current password tree bytes`() {
        expectThat(passwordTreeEnvelope.unwrap(passwordTreeEnvelope.wrap(payload))) isEqualTo payload
    }

    @Test
    fun `should return empty bytes when unwrapping empty current password tree`() {
        expectThat(passwordTreeEnvelope.unwrap(byteArrayOf())) isEqualTo byteArrayOf()
    }

    @Test
    fun `should reject unsupported bytes when unwrapping current password tree`() {
        assertThrows<IllegalStateException> {
            passwordTreeEnvelope.unwrap(passwordTreeEnvelope.wrapLegacyCurrent(payload))
        }
    }

    @Test
    fun `should unwrap legacy current password tree bytes`() {
        expectThat(passwordTreeEnvelope.unwrapLegacyCurrent(passwordTreeEnvelope.wrapLegacyCurrent(payload))) isEqualTo payload
    }

    @Test
    fun `should return empty bytes when unwrapping empty legacy current password tree`() {
        expectThat(passwordTreeEnvelope.unwrapLegacyCurrent(byteArrayOf())) isEqualTo byteArrayOf()
    }

    @Test
    fun `should reject unsupported bytes when unwrapping legacy current password tree`() {
        assertThrows<IllegalStateException> {
            passwordTreeEnvelope.unwrapLegacyCurrent(passwordTreeEnvelope.wrap(payload))
        }
    }

    @Test
    fun `should detect truncated headers as unsupported`() {
        expectThat(passwordTreeEnvelope.isCurrent(byteArrayOf(0x0, 0x50))).isFalse()
        expectThat(passwordTreeEnvelope.isLegacyCurrent(byteArrayOf(0x0, 0x50))).isFalse()
    }
}
