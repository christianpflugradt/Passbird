package de.pflugradts.passbird.domain.service

import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.InvalidKeyException
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import java.util.Optional

fun fakePasswordService(
    instance: PasswordService,
    withInvalidAlias: Boolean = false,
    withEggs: List<Egg> = emptyList(),
    withNestService: NestService? = null,
) {
    every { instance.putEgg(any(), any()) } returns Unit
    every { instance.putEggs(any()) } returns Unit
    every { instance.findAllKeys() } answers {
        if (withNestService != null) {
            withEggs
                .filter { it.associatedNest() == withNestService.getCurrentNest().slot }
                .map { it.viewKey() }.stream()
        } else {
            withEggs.map { it.viewKey() }.stream()
        }
    }
    every { instance.viewPassword(any()) } answers {
        Optional.ofNullable(withEggs.find { it.viewKey() == firstArg() }?.viewPassword())
    }
    every { instance.eggExists(any(), any<PasswordService.EggNotExistsAction>()) } answers {
        withEggs.find { it.viewKey() == firstArg() } != null
    }
    every { instance.eggExists(any(), any<Slot>()) } answers {
        val res = withEggs.find { it.viewKey() == firstArg() && it.associatedNest() == secondArg() } != null
        println(res)
        res
    }
    if (withInvalidAlias) {
        every { instance.challengeAlias(any()) } throws InvalidKeyException(emptyBytes())
    } else {
        every { instance.challengeAlias(any()) } returns Unit
    }
    every { instance.discardEgg(any()) } returns Unit
    every { instance.renameEgg(any(), any()) } returns Unit
    every { instance.moveEgg(any(), any()) } returns Unit
}
