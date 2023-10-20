package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.Tuple
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction
import java.util.stream.Stream

class PasswordFacade @Inject constructor(
    @Inject private val putPasswordService: PutPasswordService,
    @Inject private val viewPasswordService: ViewPasswordService,
    @Inject private val discardPasswordService: DiscardPasswordService,
    @Inject private val renamePasswordService: RenamePasswordService,
    @Inject private val movePasswordService: MovePasswordService,
) : PasswordService {
    override fun entryExists(keyBytes: Bytes, namespace: NamespaceSlot) = viewPasswordService.entryExists(keyBytes, namespace)
    override fun entryExists(keyBytes: Bytes, entryNotExistsAction: EntryNotExistsAction) =
        viewPasswordService.entryExists(keyBytes, entryNotExistsAction)
    override fun viewPassword(keyBytes: Bytes) = viewPasswordService.viewPassword(keyBytes)
    override fun renamePasswordEntry(keyBytes: Bytes, newKeyBytes: Bytes) = renamePasswordService.renamePasswordEntry(keyBytes, newKeyBytes)
    override fun findAllKeys() = viewPasswordService.findAllKeys()
    override fun challengeAlias(bytes: Bytes) = putPasswordService.challengeAlias(bytes)
    override fun putPasswordEntries(passwordEntries: Stream<Tuple<Bytes, Bytes>>) = putPasswordService.putPasswordEntries(passwordEntries)
    override fun putPasswordEntry(keyBytes: Bytes, passwordBytes: Bytes) = putPasswordService.putPasswordEntry(keyBytes, passwordBytes)
    override fun discardPasswordEntry(keyBytes: Bytes) = discardPasswordService.discardPasswordEntry(keyBytes)
    override fun movePasswordEntry(keyBytes: Bytes, targetNamespace: NamespaceSlot) =
        movePasswordService.movePassword(keyBytes, targetNamespace)
}
