package de.pflugradts.passbird.domain.model.namespace

import de.pflugradts.passbird.domain.model.namespace.Namespace.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.namespace.Namespace.Companion.createNamespace
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class NamespaceTest {
    @Test
    fun `should create namespace`() {
        // given
        val name = "namespace"

        // when
        val actual = createNamespace(bytesOf("namespace"), NamespaceSlot.DEFAULT)

        // then
        expectThat(actual.bytes.asString()) isEqualTo name
    }

    @Test
    fun `should clone bytes`() {
        // given
        val bytes = bytesOf("key")
        val namespace = createNamespace(bytes, NamespaceSlot.DEFAULT)

        // when
        bytes.scramble()
        val actual = namespace.bytes

        // then
        expectThat(actual) isNotEqualTo bytes
    }

    @Test
    fun `should create default namespace`() {
        // given / when
        val defaultNamespace = DEFAULT

        // then
        expectThat(defaultNamespace.bytes) isEqualTo bytesOf("Default")
        expectThat(defaultNamespace.slot) isEqualTo NamespaceSlot.DEFAULT
    }
}
