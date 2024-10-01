package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.shell.fakeEnc
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT

fun createEggForTesting(
    withEggIdShell: Shell = shellOf("EggId"),
    withPasswordShell: Shell = shellOf("Password"),
    withSlot: Slot = DEFAULT,
    withProteins: Map<Slot, ShellPair> = emptyMap(),
): Egg = createEgg(withSlot, withEggIdShell.fakeEnc(), withPasswordShell.fakeEnc()).apply {
    withProteins.forEach { updateProtein(it.key, it.value.first.fakeEnc(), it.value.second.fakeEnc()) }
}
