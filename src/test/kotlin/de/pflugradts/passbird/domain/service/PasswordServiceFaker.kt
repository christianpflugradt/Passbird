package de.pflugradts.passbird.domain.service

import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import java.util.Optional

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
                .filter { it.associatedNest() == withNestService.currentNest().nestSlot }
                .map { it.viewEggId() }.stream()
        } else {
            withEggs.map { it.viewEggId() }.stream()
        }
    }
    every { instance.viewPassword(any()) } answers {
        Optional.ofNullable(withEggs.find { it.viewEggId() == firstArg() }?.viewPassword())
    }
    every { instance.eggExists(any(), any<PasswordService.EggNotExistsAction>()) } answers {
        withEggs.find { it.viewEggId() == firstArg() } != null
    }
    every { instance.eggExists(any(), any<NestSlot>()) } answers {
        val res = withEggs.find { it.viewEggId() == firstArg() && it.associatedNest() == secondArg() } != null
        println(res)
        res
    }
    if (withInvalidEggId) {
        every { instance.challengeEggId(any()) } throws InvalidEggIdException(emptyShell())
    } else {
        every { instance.challengeEggId(any()) } returns Unit
    }
    every { instance.discardEgg(any()) } returns Unit
    every { instance.renameEgg(any(), any()) } returns Unit
    every { instance.moveEgg(any(), any()) } returns Unit
}
