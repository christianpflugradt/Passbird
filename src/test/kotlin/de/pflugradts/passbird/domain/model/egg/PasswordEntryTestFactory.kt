package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.nest.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf

fun createEggForTesting(
    withKeyBytes: Bytes = bytesOf("key"),
    withPasswordBytes: Bytes = bytesOf("password"),
    withNestSlot: Slot = DEFAULT,
): Egg = createEgg(withNestSlot, withKeyBytes, withPasswordBytes)
