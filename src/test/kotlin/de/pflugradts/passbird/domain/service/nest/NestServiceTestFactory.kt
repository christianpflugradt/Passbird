package de.pflugradts.passbird.domain.service.nest

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.kotlinextensions.toOption
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import de.pflugradts.passbird.domain.service.password.tree.NestingGround
import io.mockk.mockk
import io.mockk.spyk

fun createNestServiceForTesting() = NestingGroundService(mockk<EggRepository>(relaxed = true), mockk<EventRegistry>(relaxed = true))
fun createNestServiceSpyForTesting() = spyk(createNestServiceForTesting())

fun NestingGround.findForTesting(eggIdShell: Shell): Option<Egg> = findAll()
    .filter { it.viewEggId().fakeDec() == eggIdShell }
    .findAny().toOption()
