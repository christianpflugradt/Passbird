package de.pflugradts.passbird.domain.service

import de.pflugradts.kotlinextensions.MutableOption.Companion.optionOf
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every

fun fakePasswordService(
    instance: PasswordService,
    withInvalidEggId: Boolean = false,
    withEggs: List<Egg> = emptyList(),
    withNestService: NestService? = null,
) {
    every { instance.putEgg(any(), any()) } returns Unit
    every { instance.putEggs(any()) } returns Unit
    every { instance.findAllEggIds() } answers {
        if (withNestService != null) {
            withEggs
                .filter { it.associatedNest() == withNestService.currentNest().slot }
                .map { it.viewEggId().fakeDec() }.stream()
        } else {
            withEggs.map { it.viewEggId().fakeDec() }.stream()
        }
    }
    every { instance.viewPassword(any()) } answers {
        optionOf(withEggs.find { it.viewEggId().fakeDec() == firstArg() }?.viewPassword()?.fakeDec())
    }
    every { instance.eggExists(any(), any<PasswordService.EggNotExistsAction>()) } answers {
        withEggs.find { it.viewEggId().fakeDec() == firstArg() } != null
    }
    every { instance.eggExists(any(), any<Slot>()) } answers {
        withEggs.find { it.viewEggId().fakeDec() == firstArg() && it.associatedNest() == secondArg() } != null
    }
    every { instance.viewProteinTypes(any()) } answers {
        optionOf(withEggs.find { it.viewEggId().fakeDec() == firstArg() }?.proteins?.map { it.map { p -> p.viewType().fakeDec() } })
    }
    every { instance.viewProteinType(any(), any()) } answers {
        optionOf(
            withEggs.find {
                it.viewEggId().fakeDec() == firstArg()
            }?.proteins?.get(secondArg<Slot>().index())?.orNull()?.viewType()?.fakeDec(),
        )
    }
    every { instance.viewProteinStructures(any()) } answers {
        optionOf(withEggs.find { it.viewEggId().fakeDec() == firstArg() }?.proteins?.map { it.map { p -> p.viewStructure().fakeDec() } })
    }
    every { instance.viewProteinStructure(any(), any()) } answers {
        optionOf(
            withEggs.find {
                it.viewEggId().fakeDec() == firstArg()
            }?.proteins?.get(secondArg<Slot>().index())?.orNull()?.viewStructure()?.fakeDec(),
        )
    }
    if (withInvalidEggId) {
        every { instance.challengeEggId(any()) } answers { throw InvalidEggIdException(firstArg()) }
    } else {
        every { instance.challengeEggId(any()) } returns Unit
    }
    every { instance.discardEgg(any()) } returns Unit
    every { instance.renameEgg(any(), any()) } returns Unit
    every { instance.moveEgg(any(), any()) } returns Unit
}
