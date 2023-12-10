package de.pflugradts.passbird.domain.service

import de.pflugradts.passbird.domain.model.namespace.Namespace.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import strikt.java.isPresent
import kotlin.jvm.optionals.getOrNull

class FixedNamespaceServiceTest {

    private val passwordEntryRepository = mockk<PasswordEntryRepository>(relaxed = true)
    private val namespaceService = FixedNamespaceService(passwordEntryRepository)

    @Test
    fun `should have 9 empty slots upon initialisation`() {
        // given / when
        val actual = namespaceService.all().toList()

        // then
        expectThat(actual) hasSize CAPACITY
        expectThat(actual.stream().allMatch { it.isEmpty }).isTrue()
    }

    @Test
    fun `should populate namespaces`() {
        // given
        val namespaceBytes = listOf(
            emptyBytes(), bytesOf("namespace1"), emptyBytes(), bytesOf("namespace3"),
            emptyBytes(), emptyBytes(), emptyBytes(), bytesOf("namespace7"), emptyBytes(),
        )

        // when
        namespaceService.populate(namespaceBytes)
        val actual = namespaceService.all().toList()

        // then
        intArrayOf(1, 3, 7).forEach {
            expectThat(actual[it].isPresent).isTrue()
            expectThat(actual[it].get().bytes) isEqualTo namespaceBytes[it]
        }
        intArrayOf(0, 2, 4, 5, 6, 8).forEach { expectThat(actual[it].isPresent).isFalse() }
    }

    @Test
    fun `should not populate namespaces if number of namespaces does not match`() {
        // given
        val namespaceBytes = listOf(bytesOf("namespace1"), bytesOf("namespace2"), bytesOf("namespace3"))

        // when
        namespaceService.populate(namespaceBytes)
        val actual = namespaceService.all().toList()

        // then
        (0..<9).forEach {
            expectThat(actual[it].isEmpty).isTrue()
        }
    }

    @Test
    fun `should return default namespace for default slot`() {
        // given / when / then
        expectThat(namespaceService.atSlot(NamespaceSlot.DEFAULT).getOrNull()) isEqualTo DEFAULT
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
        namespaceService.populate(namespaceBytes)

        // then
        val namespace2 = namespaceService.atSlot(NamespaceSlot.N2)
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
        namespaceService.populate(namespaceBytes)

        // then
        expectThat(namespaceService.atSlot(NamespaceSlot.N1).isPresent).isFalse()
    }

    @Test
    fun `should return default namespace if none is set`() {
        // given
        val namespaceBytes = listOf(
            emptyBytes(), bytesOf("slot2"), emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )

        // when
        namespaceService.populate(namespaceBytes)

        // then
        expectThat(namespaceService.getCurrentNamespace().slot) isEqualTo NamespaceSlot.DEFAULT
    }

    @Test
    fun `should update and return current namespace`() {
        // given
        val namespaceBytes = listOf(
            emptyBytes(), bytesOf("slot2"), emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )
        namespaceService.populate(namespaceBytes)
        val wantedCurrentNamespace = NamespaceSlot.N2

        // when
        namespaceService.updateCurrentNamespace(wantedCurrentNamespace)

        // then
        expectThat(namespaceService.getCurrentNamespace().slot) isEqualTo wantedCurrentNamespace
    }

    @Test
    fun `should not update anything if namespace is not deployed`() {
        // given
        val namespaceBytes = listOf(
            emptyBytes(), bytesOf("slot2"), emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )
        namespaceService.populate(namespaceBytes)
        val wantedCurrentNamespace = NamespaceSlot.N1

        // when
        namespaceService.updateCurrentNamespace(wantedCurrentNamespace)

        // then
        expectThat(namespaceService.getCurrentNamespace().slot) isNotEqualTo wantedCurrentNamespace
    }

    @Test
    fun `should deploy namespace and sync`() {
        // given
        val namespaceBytes = bytesOf("name space")

        // when
        namespaceService.deploy(namespaceBytes, NamespaceSlot.N3)
        val actual = namespaceService.atSlot(NamespaceSlot.N3)

        // then
        expectThat(actual.isPresent).isTrue()
        expectThat(actual.get().bytes) isEqualTo namespaceBytes
        expectThat(actual.get().slot) isEqualTo NamespaceSlot.N3
        verify(exactly = 1) { passwordEntryRepository.sync() }
    }
}
