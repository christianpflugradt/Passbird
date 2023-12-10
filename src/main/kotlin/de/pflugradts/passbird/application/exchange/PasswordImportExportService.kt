package de.pflugradts.passbird.application.exchange

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService

class PasswordImportExportService @Inject constructor(
    @Inject private val exchangeFactory: ExchangeFactory,
    @Inject private val passwordService: PasswordService,
    @Inject private val nestService: NestService,
) : ImportExportService {
    override fun peekImportKeyBytes(uri: String) = exchangeFactory.createPasswordExchange(uri).receive().entries.associate {
        it.key to it.value.map { bytePair -> bytePair.value.first }
    }

    override fun importPasswordEntries(uri: String) {
        val currentNest = nestService.getCurrentNest()
        val passwordEntriesByNest = exchangeFactory.createPasswordExchange(uri).receive()
        passwordEntriesByNest.keys.forEach { slot ->
            val deployedNest = nestService.atSlot(slot)
            if (deployedNest.isEmpty) {
                nestService.deploy(bytesOf("Namespace-${slot.index()}"), slot)
            }
            nestService.moveToNestAt(slot)
            passwordService.putPasswordEntries(passwordEntriesByNest[slot]!!.stream())
        }
        nestService.moveToNestAt(currentNest.slot)
    }

    override fun exportPasswordEntries(uri: String) {
        val currentNest = nestService.getCurrentNest()
        val passwordEntriesByNest = mutableMapOf<Slot, List<BytePair>>()
        nestService.all(includeDefault = true).filter { it.isPresent }.map { it.get() }.forEach { nest ->
            nestService.moveToNestAt(nest.slot)
            passwordEntriesByNest[nest.slot] = passwordService.findAllKeys()
                .map { key -> BytePair(Pair(key, passwordService.viewPassword(key).get())) }.toList()
        }
        exchangeFactory.createPasswordExchange(uri).send(passwordEntriesByNest)
        nestService.moveToNestAt(currentNest.slot)
    }
}
