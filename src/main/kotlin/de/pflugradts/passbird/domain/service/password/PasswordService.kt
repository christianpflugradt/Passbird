package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import java.util.Optional
import java.util.stream.Stream

interface PasswordService {
    enum class EggNotExistsAction {
        DO_NOTHING,
        CREATE_ENTRY_NOT_EXISTS_EVENT,
    }

    fun eggExists(eggIdBytes: Bytes, nestSlot: Slot): Boolean
    fun eggExists(eggIdBytes: Bytes, eggNotExistsAction: EggNotExistsAction): Boolean
    fun viewPassword(eggIdBytes: Bytes): Optional<Bytes>
    fun renameEgg(eggIdBytes: Bytes, newEggIdBytes: Bytes)
    fun challengeEggId(bytes: Bytes)
    fun putEggs(eggs: Stream<BytePair>)
    fun putEgg(eggIdBytes: Bytes, passwordBytes: Bytes)
    fun discardEgg(eggIdBytes: Bytes)
    fun moveEgg(eggIdBytes: Bytes, targetNestSlot: Slot)
    fun findAllEggIds(): Stream<Bytes>
}
