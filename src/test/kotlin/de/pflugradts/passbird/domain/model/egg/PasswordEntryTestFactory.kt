package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT

fun createEggForTesting(
    withEggIdShell: Shell = shellOf("EggId"),
    withPasswordShell: Shell = shellOf("Password"),
    withSlot: Slot = DEFAULT,
): Egg = createEgg(withSlot, withEggIdShell, withPasswordShell)
