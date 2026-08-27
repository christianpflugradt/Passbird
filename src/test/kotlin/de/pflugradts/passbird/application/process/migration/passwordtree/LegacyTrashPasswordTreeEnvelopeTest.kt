package de.pflugradts.passbird.application.process.migration.passwordtree

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class LegacyTrashPasswordTreeEnvelopeTest {

    private val payload = "payload".toByteArray()

    @Test
    fun `should unwrap legacy trash password tree bytes`() {
        expectThat(unwrapLegacyTrashPasswordTree(wrapLegacyTrashPasswordTree(payload))) isEqualTo payload
    }

    @Test
    fun `should return empty bytes when unwrapping empty legacy trash password tree`() {
        expectThat(unwrapLegacyTrashPasswordTree(byteArrayOf())) isEqualTo byteArrayOf()
    }

    @Test
    fun `should reject unsupported bytes when unwrapping legacy trash password tree`() {
        assertThrows<IllegalStateException> {
            unwrapLegacyTrashPasswordTree(byteArrayOf(0x0, 0x50))
        }
    }

    @Test
    fun `should detect legacy trash headers accurately`() {
        expectThat(isLegacyTrashPasswordTree(wrapLegacyTrashPasswordTree(payload))).isTrue()
        expectThat(isLegacyTrashPasswordTree(byteArrayOf(0x0, 0x50))).isFalse()
    }
}
