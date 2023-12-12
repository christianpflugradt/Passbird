package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import java.util.stream.Stream

class PasswordFacade @Inject constructor(
    @Inject private val putPasswordService: PutPasswordService,
    @Inject private val viewPasswordService: ViewPasswordService,
    @Inject private val discardPasswordService: DiscardPasswordService,
    @Inject private val renamePasswordService: RenamePasswordService,
    @Inject private val movePasswordService: MovePasswordService,
) : PasswordService {
    override fun eggExists(eggIdBytes: Bytes, nestSlot: Slot) = viewPasswordService.eggExists(eggIdBytes, nestSlot)
    override fun eggExists(eggIdBytes: Bytes, eggNotExistsAction: EggNotExistsAction) =
        viewPasswordService.eggExists(eggIdBytes, eggNotExistsAction)
    override fun viewPassword(eggIdBytes: Bytes) = viewPasswordService.viewPassword(eggIdBytes)
    override fun renameEgg(eggIdBytes: Bytes, newEggIdBytes: Bytes) = renamePasswordService.renameEgg(eggIdBytes, newEggIdBytes)
    override fun findAllEggIds() = viewPasswordService.findAllEggIds()
    override fun challengeEggId(bytes: Bytes) = putPasswordService.challengeEggId(bytes)
    override fun putEggs(eggs: Stream<BytePair>) = putPasswordService.putEggs(eggs)
    override fun putEgg(eggIdBytes: Bytes, passwordBytes: Bytes) = putPasswordService.putEgg(eggIdBytes, passwordBytes)
    override fun discardEgg(eggIdBytes: Bytes) = discardPasswordService.discardEgg(eggIdBytes)
    override fun moveEgg(eggIdBytes: Bytes, targetNestSlot: Slot) =
        movePasswordService.movePassword(eggIdBytes, targetNestSlot)
}
