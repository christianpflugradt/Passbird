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

    fun eggExists(keyBytes: Bytes, nestSlot: Slot): Boolean
    fun eggExists(keyBytes: Bytes, eggNotExistsAction: EggNotExistsAction): Boolean
    fun viewPassword(keyBytes: Bytes): Optional<Bytes>
    fun renameEgg(keyBytes: Bytes, newKeyBytes: Bytes)
    fun challengeAlias(bytes: Bytes)
    fun putEggs(eggs: Stream<BytePair>)
    fun putEgg(keyBytes: Bytes, passwordBytes: Bytes)
    fun discardEgg(keyBytes: Bytes)
    fun moveEgg(keyBytes: Bytes, targetNestSlot: Slot)
    fun findAllKeys(): Stream<Bytes>
}
