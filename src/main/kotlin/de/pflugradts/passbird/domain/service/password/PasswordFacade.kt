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
    override fun eggExists(keyBytes: Bytes, nestSlot: Slot) = viewPasswordService.eggExists(keyBytes, nestSlot)
    override fun eggExists(keyBytes: Bytes, eggNotExistsAction: EggNotExistsAction) =
        viewPasswordService.eggExists(keyBytes, eggNotExistsAction)
    override fun viewPassword(keyBytes: Bytes) = viewPasswordService.viewPassword(keyBytes)
    override fun renameEgg(keyBytes: Bytes, newKeyBytes: Bytes) = renamePasswordService.renameEgg(keyBytes, newKeyBytes)
    override fun findAllKeys() = viewPasswordService.findAllKeys()
    override fun challengeAlias(bytes: Bytes) = putPasswordService.challengeAlias(bytes)
    override fun putEggs(eggs: Stream<BytePair>) = putPasswordService.putEggs(eggs)
    override fun putEgg(keyBytes: Bytes, passwordBytes: Bytes) = putPasswordService.putEgg(keyBytes, passwordBytes)
    override fun discardEgg(keyBytes: Bytes) = discardPasswordService.discardEgg(keyBytes)
    override fun moveEgg(keyBytes: Bytes, targetNestSlot: Slot) =
        movePasswordService.movePassword(keyBytes, targetNestSlot)
}
