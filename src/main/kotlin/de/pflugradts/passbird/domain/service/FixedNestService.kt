package de.pflugradts.passbird.domain.service

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.nest.Slot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.nest.Slot.Companion.FIRST_SLOT
import de.pflugradts.passbird.domain.model.nest.Slot.Companion.LAST_SLOT
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import java.util.Collections
import java.util.Optional

class FixedNestService @Inject constructor(
    @Inject private val passwordEntryRepository: PasswordEntryRepository,
) : NestService {

    private val nests = Collections.nCopies(CAPACITY, Optional.empty<Nest>()).toMutableList()
    private var currentNest = Slot.DEFAULT

    override fun populate(nestBytes: List<Bytes>) {
        if (nestBytes.size == CAPACITY) {
            (FIRST_SLOT..LAST_SLOT).forEach {
                if (nestBytes[it - 1].isNotEmpty) {
                    nests[it - 1] = Optional.of(Nest.createNest(nestBytes[it - 1], Slot.at(it)))
                }
            }
        }
    }

    override fun deploy(nestBytes: Bytes, slot: Slot) {
        nests[slot.index() - 1] = Optional.of(Nest.createNest(nestBytes, slot))
        passwordEntryRepository.sync()
    }
    override fun atSlot(slot: Slot): Optional<Nest> =
        if (slot === Slot.DEFAULT) Optional.of(DEFAULT) else nests[slot.index() - 1]
    override fun all(includeDefault: Boolean) = nests.let { if (includeDefault) DEFAULT.asOptionalInList() + it else it }.stream()
    override fun getCurrentNest(): Nest = atSlot(currentNest).orElse(DEFAULT)
    override fun moveToNestAt(slot: Slot) {
        if (atSlot(slot).isPresent) { currentNest = slot }
    }
}

private fun Nest.asOptionalInList() = listOf(Optional.of(this))
