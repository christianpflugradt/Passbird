package de.pflugradts.passbird.application.process.migration.passwordtree

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class LegacyCurrentPasswordTreeEnvelopeTest {
    private val payload = "payload".toByteArray()

    @Test
    fun `should wrap and unwrap legacy current password tree bytes`() {
        expectThat(unwrapLegacyCurrentPasswordTree(wrapLegacyCurrentPasswordTree(payload))) isEqualTo payload
    }

    @Test
    fun `should return empty bytes when unwrapping empty legacy current password tree`() {
        expectThat(unwrapLegacyCurrentPasswordTree(byteArrayOf())) isEqualTo byteArrayOf()
    }

    @Test
    fun `should reject unsupported bytes when unwrapping legacy current password tree`() {
        assertThrows<IllegalStateException> {
            unwrapLegacyCurrentPasswordTree(payload)
        }
    }

    @Test
    fun `should detect legacy current password tree headers`() {
        expectThat(isLegacyCurrentPasswordTree(wrapLegacyCurrentPasswordTree(payload))).isTrue()
        expectThat(isLegacyCurrentPasswordTree(byteArrayOf(0x0, 0x50))).isFalse()
    }
}
