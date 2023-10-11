package de.pflugradts.passbird.domain.model.namespace

import de.pflugradts.passbird.domain.model.namespace.Namespace.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import strikt.java.isPresent
import java.util.Collections
import kotlin.jvm.optionals.getOrNull

class NamespacesTest {
    private val namespaces = Namespaces()

    @BeforeEach
    fun reset() {
        namespaces.reset()
    }

    @Test
    fun `should have 9 slots`() {
        // given // when // then
        expectThat(namespaces.all().toList()) hasSize CAPACITY
    }

    @Test
    fun `should populate namespaces`() {
        // given
        val namespaceBytes = listOf(
            emptyBytes(), bytesOf("namespace1"), emptyBytes(), bytesOf("namespace3"),
            emptyBytes(), emptyBytes(), emptyBytes(), bytesOf("namespace7"), emptyBytes(),
        )

        // when
        namespaces.populate(namespaceBytes)
        val actual = namespaces.all().toList()

        // then
        intArrayOf(1, 3, 7).forEach {
            expectThat(actual[it].isPresent).isTrue()
            expectThat(actual[it].get().bytes) isEqualTo namespaceBytes[it]
        }
        intArrayOf(0, 2, 4, 5, 6, 8).forEach { expectThat(actual[it].isPresent).isFalse() }
    }

    @Test
    fun `should populate only once`() {
        // given
        val givenBytes = bytesOf("namespace")
        val otherBytes = bytesOf("namespaceOthers")
        val namespaceBytes = Collections.nCopies(9, givenBytes)

        // when
        namespaces.populate(namespaceBytes)
        namespaces.populate(Collections.nCopies(9, otherBytes))
        val actual = namespaces.all().toList()

        // then
        actual.forEach { expectThat(it.getOrNull()?.bytes) isEqualTo givenBytes }
    }

    @Test
    fun `should return default namespace for default slot`() {
        // given / when / then
        expectThat(namespaces.atSlot(NamespaceSlot.DEFAULT).getOrNull()) isEqualTo DEFAULT
    }

    @Test
    fun `should return namespace for non empty slot`() {
        // given
        val givenNamespaceBytes = bytesOf("slot2")
        val namespaceBytes = listOf(
            emptyBytes(), givenNamespaceBytes, emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )

        // when
        namespaces.populate(namespaceBytes)

        // then
        val namespace2 = namespaces.atSlot(NamespaceSlot.N2)
        expectThat(namespace2).isPresent()
        expectThat(namespace2.get().slot) isEqualTo NamespaceSlot.N2
        expectThat(namespace2.get().bytes) isEqualTo givenNamespaceBytes
    }

    @Test
    fun `should return empty optional for empty slot`() {
        // given
        val namespaceBytes = listOf(
            emptyBytes(), bytesOf("slot2"), emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )

        // when
        namespaces.populate(namespaceBytes)

        // then
        expectThat(namespaces.atSlot(NamespaceSlot.N1).isPresent).isFalse()
    }

    @Test
    fun `should return default namespace if none is set`() {
        // given
        val namespaceBytes = listOf(
            emptyBytes(), bytesOf("slot2"), emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )

        // when
        namespaces.populate(namespaceBytes)

        // then
        expectThat(namespaces.getCurrentNamespace().slot) isEqualTo NamespaceSlot.DEFAULT
    }

    @Test
    fun `should update and return current namespace`() {
        // given
        val namespaceBytes = listOf(
            emptyBytes(), bytesOf("slot2"), emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )
        namespaces.populate(namespaceBytes)
        val wantedCurrentNamespace = NamespaceSlot.N2

        // when
        namespaces.updateCurrentNamespace(wantedCurrentNamespace)

        // then
        expectThat(namespaces.getCurrentNamespace().slot) isEqualTo wantedCurrentNamespace
    }

    @Test
    fun `should not update anything if namespace is not deployed`() {
        // given
        val namespaceBytes = listOf(
            emptyBytes(), bytesOf("slot2"), emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )
        namespaces.populate(namespaceBytes)
        val wantedCurrentNamespace = NamespaceSlot.N1

        // when
        namespaces.updateCurrentNamespace(wantedCurrentNamespace)

        // then
        expectThat(namespaces.getCurrentNamespace().slot) isNotEqualTo wantedCurrentNamespace
    }
}
