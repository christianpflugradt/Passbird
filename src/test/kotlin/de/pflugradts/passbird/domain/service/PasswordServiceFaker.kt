package de.pflugradts.passbird.domain.service

import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.optionOf
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.egg.Protein
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slots
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.NestStats
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.RestoreEggResult
import de.pflugradts.passbird.domain.service.password.TrashEggView
import de.pflugradts.passbird.domain.service.password.YolkView
import io.mockk.every

fun fakePasswordService(
    instance: PasswordService,
    withInvalidEggId: Boolean = false,
    withEggs: List<Egg> = emptyList(),
    withNestService: NestService? = null,
    withFavorites: Map<Slot, String> = emptyMap(),
    withNestFavoriteCounts: Map<Slot, Int> = emptyMap(),
    withMemory: Map<Slot, String> = emptyMap(),
) {
    fakeWriteOperations(instance)
    fakeEggQueries(instance, withEggs, withNestService, withFavorites, withNestFavoriteCounts)
    fakeProteinQueries(instance, withEggs)
    fakeEggValidation(instance, withInvalidEggId)
    fakeRemainingQueries(instance, withEggs, withFavorites, withMemory)
}

private fun fakeWriteOperations(instance: PasswordService) {
    every { instance.putEgg(any(), any()) } returns success(Unit)
    every { instance.putEggs(any()) } returns success(Unit)
    every { instance.putProtein(any(), any(), any(), any()) } returns success(Unit)
    every { instance.putProteins(any(), any()) } returns success(Unit)
    every { instance.putYolk(any(), any(), any<String>(), any(), any()) } returns success(Unit)
}

private fun fakeEggQueries(
    instance: PasswordService,
    withEggs: List<Egg>,
    withNestService: NestService?,
    withFavorites: Map<Slot, String>,
    withNestFavoriteCounts: Map<Slot, Int>,
) {
    every { instance.findAllEggIds() } answers {
        if (withNestService != null) {
            withEggs
                .filter { it.associatedNest() == withNestService.currentNest().slot }
                .map { it.viewEggId().fakeDec() }.stream()
        } else {
            withEggs.map { it.viewEggId().fakeDec() }.stream()
        }
    }
    every { instance.findAllEggIds(any<Slot>()) } answers {
        withEggs
            .filter { it.associatedNest() == firstArg() }
            .map { it.viewEggId().fakeDec() }.stream()
    }
    every { instance.viewPassword(any()) } answers {
        optionOf(withEggs.find { it.viewEggId().fakeDec() == firstArg() }?.viewPassword()?.fakeDec())
    }
    every { instance.viewNestStats() } answers {
        val currentNestSlot = withNestService?.currentNest()?.slot ?: Slot.DEFAULT
        statsFor(
            withEggs.filter { it.associatedNest() == currentNestSlot },
            withNestFavoriteCounts[currentNestSlot] ?: withFavorites.size,
        )
    }
    every { instance.viewNestStats(any<Slot>()) } answers {
        val slot = firstArg<Slot>()
        val assignedFavorites = withNestFavoriteCounts[slot]
            ?: if (withNestService?.currentNest()?.slot == slot) withFavorites.size else 0
        statsFor(withEggs.filter { it.associatedNest() == slot }, assignedFavorites)
    }
    every { instance.eggExists(any(), any<PasswordService.EggNotExistsAction>()) } answers {
        withEggs.find { it.viewEggId().fakeDec() == firstArg() } != null
    }
    every { instance.eggExists(any(), any<Slot>()) } answers {
        withEggs.find { it.viewEggId().fakeDec() == firstArg() && it.associatedNest() == secondArg() } != null
    }
}

private fun fakeProteinQueries(instance: PasswordService, withEggs: List<Egg>) {
    every { instance.proteinExists(any(), any<Slot>()) } answers {
        withEggs.find { it.viewEggId().fakeDec() == firstArg() && it.proteins[secondArg<Slot>().index()].isPresent } != null
    }
    every { instance.viewProteinTypes(any()) } answers {
        optionOf(withEggs.find { it.viewEggId().fakeDec() == firstArg() }?.proteins?.map { it.map { p -> p.viewType().fakeDec() } })
    }
    every { instance.viewProteinType(any(), any()) } answers {
        optionOf(
            withEggs.find {
                it.viewEggId().fakeDec() == firstArg()
            }?.proteins?.get(secondArg<Slot>().index())?.extractType(),
        )
    }
    every { instance.viewProteinStructures(any()) } answers {
        optionOf(withEggs.find { it.viewEggId().fakeDec() == firstArg() }?.proteins?.map { it.map { p -> p.viewStructure().fakeDec() } })
    }
    every { instance.viewProteinStructure(any(), any()) } answers {
        optionOf(
            withEggs.find {
                it.viewEggId().fakeDec() == firstArg()
            }?.proteins?.get(secondArg<Slot>().index())?.extractStructure(),
        )
    }
    every { instance.viewYolk(any()) } answers {
        optionOf(
            withEggs.find { it.viewEggId().fakeDec() == firstArg() }?.viewYolk()?.map {
                YolkView(
                    secret = it.viewSecret().fakeDec(),
                    algorithm = it.algorithm,
                    digits = it.digits,
                    periodSeconds = it.periodSeconds,
                )
            }?.orNull(),
        )
    }
}

private fun fakeEggValidation(instance: PasswordService, withInvalidEggId: Boolean) {
    if (withInvalidEggId) {
        every { instance.challengeEggId(any()) } answers { throw InvalidEggIdException(firstArg()) }
    } else {
        every { instance.challengeEggId(any()) } returns Unit
    }
}

private fun fakeRemainingQueries(
    instance: PasswordService,
    withEggs: List<Egg>,
    withFavorites: Map<Slot, String>,
    withMemory: Map<Slot, String>,
) {
    every { instance.putFavorite(any(), any()) } returns success(Unit)
    every { instance.discardFavorite(any()) } returns success(Unit)
    every { instance.discardEgg(any()) } returns success(Unit)
    every { instance.discardEggPermanently(any()) } returns success(Unit)
    every { instance.cleanupTrash(any()) } returns success(0)
    every { instance.discardProtein(any(), any()) } returns success(Unit)
    every { instance.discardYolk(any()) } returns success(Unit)
    every { instance.renameEgg(any(), any()) } returns success(Unit)
    every { instance.moveEgg(any(), any()) } returns success(Unit)
    every { instance.restoreEgg(any()) } returns success(RestoreEggResult.RESTORED)
    every { instance.viewTrash() } answers {
        withEggs.filter(Egg::isTrashed).map { egg ->
            TrashEggView(
                eggId = egg.viewEggId().fakeDec(),
                nestSlot = egg.associatedNest(),
                deletionAgeDays = 0,
            )
        }
    }
    every { instance.viewFavorites() } answers { Slots<Shell>().apply { withFavorites.forEach { this[it.key] = shellOf(it.value) } } }
    every { instance.viewFavoriteEntry(any<Slot>()) } answers { instance.viewFavorites()[firstArg<Slot>()] }
    every { instance.viewMemory() } answers { Slots<Shell>().apply { withMemory.forEach { this[it.key] = shellOf(it.value) } } }
    every { instance.viewMemoryEntry(any<Slot>()) } answers { instance.viewMemory()[firstArg<Slot>()] }
}

private fun MutableOption<Protein>.extractType() = orNull()?.viewType()?.fakeDec() ?: emptyShell()
private fun MutableOption<Protein>.extractStructure() = orNull()?.viewStructure()?.fakeDec() ?: emptyShell()
private fun statsFor(eggs: List<Egg>, assignedFavorites: Int) = NestStats(
    eggs = eggs.size,
    eggsWithYolks = eggs.count(Egg::hasYolk),
    eggsWithProteins = eggs.count { egg -> egg.proteins.any { it.isPresent } },
    occupiedProteinSlots = eggs.sumOf { egg -> egg.proteins.count { it.isPresent } },
    assignedFavorites = assignedFavorites,
)
