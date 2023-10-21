package de.pflugradts.passbird.application.exchange

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.password.PasswordService
import java.util.stream.Stream

class PasswordImportExportService @Inject constructor(
    @Inject private val exchangeFactory: ExchangeFactory,
    @Inject private val passwordService: PasswordService,
) : ImportExportService {
    override fun peekImportKeyBytes(uri: String): Stream<Bytes> =
        exchangeFactory.createPasswordExchange(uri).receive().map { it.value.first }

    override fun importPasswordEntries(uri: String) {
        passwordService.putPasswordEntries(
            exchangeFactory.createPasswordExchange(uri).receive(),
            // FIXME when migrated password service to kotlin
        )
    }

    override fun exportPasswordEntries(uri: String) {
        passwordService.findAllKeys().map { retrievePasswordEntry(it) }.toList().let {
            if (it.isNotEmpty()) { exchangeFactory.createPasswordExchange(uri).send(it.stream()) }
        }
    }

    private fun retrievePasswordEntry(keyBytes: Bytes) = passwordService.viewPassword(keyBytes).map { BytePair(Pair(keyBytes, it)) }.get()
}
