package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.nest.NestSlot.DEFAULT
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf

fun createEggForTesting(
    withEggIdShell: Shell = shellOf("eggId"),
    withPasswordShell: Shell = shellOf("password"),
    withNestSlot: NestSlot = DEFAULT,
): Egg = createEgg(withNestSlot, withEggIdShell, withPasswordShell)
