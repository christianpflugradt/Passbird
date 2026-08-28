package de.pflugradts.passbird.domain.service.nest

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.kotlinextensions.toOption
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.tree.NestingGround
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeAdapterPort
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeSyncService
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk

fun createNestServiceForTesting() = NestingGroundService(
    mockk<PasswordTreeAdapterPort>(relaxed = true),
    mockk<PasswordTreeSyncService>(relaxed = true).also { every { it.sync() } returns success(Unit) },
    mockk<EventRegistry>(relaxed = true),
    { mockk(relaxed = true) },
)
fun createNestServiceSpyForTesting() = spyk(createNestServiceForTesting())

fun NestingGround.findForTesting(eggIdShell: Shell): Option<Egg> = findAll()
    .filter { it.viewEggId().fakeDec() == eggIdShell }
    .findAny().toOption()
