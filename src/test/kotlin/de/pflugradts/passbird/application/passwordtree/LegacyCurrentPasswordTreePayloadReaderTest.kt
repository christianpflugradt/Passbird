package de.pflugradts.passbird.application.passwordtree

import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.Protein.Companion.createProtein
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.password.tree.emptyFavorites
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse

class LegacyCurrentPasswordTreePayloadReaderTest {
    private val configuration = mockk<Configuration>()
    private val systemOperation = mockk<de.pflugradts.passbird.application.util.SystemOperation>(relaxed = true)
    private val legacyCurrentPasswordTreePayloadWriter = LegacyCurrentPasswordTreePayloadWriter()
    private val cryptoProvider = createAesGcmCipherForTesting()

    @Test
    fun `should return empty snapshot for empty payload`() {
        fakeConfiguration(instance = configuration)

        val actual = LegacyCurrentPasswordTreePayloadReader(configuration, systemOperation).read(emptyShell())

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

        val actual = LegacyCurrentPasswordTreePayloadReader(configuration, systemOperation).read(
            legacyCurrentPasswordTreePayloadWriter.write(createSnapshotWithMemoryFavoritesAndNests()),
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

        val actual = LegacyCurrentPasswordTreePayloadReader(configuration, systemOperation).read(
            legacyCurrentPasswordTreePayloadWriter.write(createSnapshotWithMemoryFavoritesAndNests()),
        )

        expectThat(actual.memory[Slot.DEFAULT].get().any { it.isPresent }).isFalse()
    }

    @Test
    fun `should restore eggs proteins favorites memory and nests from legacy current payload`() {
        fakeConfiguration(
            instance = configuration,
            withEggIdMemoryEnabled = true,
            withEggIdMemoryPersisted = true,
        )

        val snapshot = createSnapshotWithMemoryFavoritesAndNests()
        val actual = LegacyCurrentPasswordTreePayloadReader(configuration, systemOperation).read(
            legacyCurrentPasswordTreePayloadWriter.write(snapshot),
        )

        expectThat(actual.eggs).hasSize(1)
        expectThat(actual.nests[Slot.DEFAULT.index()].asString()) isEqualTo "Default"
        expectThat(actual.nests[Slot.S2.index()].asString()) isEqualTo "Work"
        expectThat(actual.memory[Slot.DEFAULT].get()[0].get()).isEqualTo(snapshot.memory[Slot.DEFAULT].get()[0].get())
        expectThat(actual.favorites[Slot.DEFAULT].get()[Slot.S1.index()].get())
            .isEqualTo(snapshot.favorites[Slot.DEFAULT].get()[Slot.S1.index()].get())
        expectThat(cryptoProvider.decrypt(actual.eggs.single().proteins[Slot.S3.index()].get().viewType()).asString()) isEqualTo "type3"
        expectThat(cryptoProvider.decrypt(actual.eggs.single().proteins[Slot.S3.index()].get().viewStructure()).asString())
            .isEqualTo("structure3")
    }

    private fun createSnapshotWithMemoryFavoritesAndNests(): PasswordTreeSnapshot {
        val egg = createEgg(
            slot = Slot.DEFAULT,
            eggIdShell = cryptoProvider.encrypt(shellOf("email")),
            passwordShell = cryptoProvider.encrypt(shellOf("Password1")),
            proteins = List(10) { index ->
                if (index == Slot.S3.index()) {
                    mutableOptionOf(
                        createProtein(
                            cryptoProvider.encrypt(shellOf("type3")),
                            cryptoProvider.encrypt(shellOf("structure3")),
                        ),
                    )
                } else {
                    mutableOptionOf()
                }
            },
        )
        val eggId = egg.viewEggId()
        return try {
            PasswordTreeSnapshot(
                eggs = listOf(egg),
                favorites = emptyFavorites().apply {
                    this[Slot.DEFAULT].get().assign(Slot.S1, eggId)
                },
                memory = emptyMemory().apply {
                    this[Slot.DEFAULT].get().memorize(eggId, null)
                },
                nests = List(Slot.CAPACITY) { slot ->
                    when (slot) {
                        Slot.DEFAULT.index() -> shellOf("Default")
                        Slot.S2.index() -> shellOf("Work")
                        else -> emptyShell()
                    }
                },
            )
        } finally {
            eggId.scramble()
        }
    }
}
