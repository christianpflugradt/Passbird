package de.pflugradts.passbird.application.passwordtree

import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isFalse

class LegacyPasswordTreePayloadReaderTest {
    private val configuration = mockk<Configuration>()
    private val systemOperation = mockk<de.pflugradts.passbird.application.util.SystemOperation>(relaxed = true)
    private val legacyPasswordTreePayloadWriter = LegacyPasswordTreePayloadWriter()
    private val cryptoProvider = createAesGcmCipherForTesting()

    @Test
    fun `should return empty snapshot for empty payload`() {
        fakeConfiguration(instance = configuration)

        val actual = LegacyPasswordTreePayloadReader(configuration, systemOperation).read(emptyShell())

        expectThat(actual.eggs).hasSize(0)
        expectThat(actual.memory[Slot.DEFAULT].get().any { it.isPresent }).isFalse()
        expectThat(actual.favorites[Slot.DEFAULT].get().any { it.isPresent }).isFalse()
    }

    @Test
    fun `should ignore restored memory when egg id memory is disabled`() {
        fakeConfiguration(
            instance = configuration,
            withEggIdMemoryEnabled = false,
            withEggIdMemoryPersisted = true,
        )

        val actual = LegacyPasswordTreePayloadReader(configuration, systemOperation).read(
            legacyPasswordTreePayloadWriter.write(createLegacySnapshotWithMemory()),
        )

        expectThat(actual.memory[Slot.DEFAULT].get().any { it.isPresent }).isFalse()
    }

    @Test
    fun `should ignore restored memory when egg id memory is not persisted`() {
        fakeConfiguration(
            instance = configuration,
            withEggIdMemoryEnabled = true,
            withEggIdMemoryPersisted = false,
        )

        val actual = LegacyPasswordTreePayloadReader(configuration, systemOperation).read(
            legacyPasswordTreePayloadWriter.write(createLegacySnapshotWithMemory()),
        )

        expectThat(actual.memory[Slot.DEFAULT].get().any { it.isPresent }).isFalse()
    }

    private fun createLegacySnapshotWithMemory(): PasswordTreeSnapshot {
        val egg = createEgg(
            slot = Slot.DEFAULT,
            eggIdShell = cryptoProvider.encrypt(shellOf("email")),
            passwordShell = cryptoProvider.encrypt(shellOf("Password1")),
        )
        return PasswordTreeSnapshot(
            eggs = listOf(egg),
            memory = emptyMemory().apply {
                this[Slot.DEFAULT].get().memorize(egg.viewEggId(), null)
            },
        )
    }
}
