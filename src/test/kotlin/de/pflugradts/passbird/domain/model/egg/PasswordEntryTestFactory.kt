package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.nest.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf

fun createEggForTesting(
    withEggIdBytes: Bytes = bytesOf("eggId"),
    withPasswordBytes: Bytes = bytesOf("password"),
    withNestSlot: Slot = DEFAULT,
): Egg = createEgg(withNestSlot, withEggIdBytes, withPasswordBytes)
