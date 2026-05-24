package de.pflugradts.passbird.adapter.exchange

import de.pflugradts.passbird.PROPERTY
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.property.ExchangeFixture
import de.pflugradts.passbird.property.exchangeFixtures
import de.pflugradts.passbird.property.normalizePasswordInfoMap
import de.pflugradts.passbird.property.toPasswordInfoMap
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.Tag
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import java.nio.file.Files

@Tag(PROPERTY)
class FilePasswordExchangePropertyTest {

    @Property(tries = 25)
    fun preservesExchangeDataAcrossExportAndImport(@ForAll("fixtures") fixture: ExchangeFixture) {
        val homeDirectory = Files.createTempDirectory("passbird-exchange-property")

        try {
            val filePasswordExchange = FilePasswordExchange(
                SystemOperation(),
                PassbirdRunContext(homeDirectory.toString().toDirectory(), Slot.DEFAULT),
            )

            val sendResult = filePasswordExchange.send(fixture.toPasswordInfoMap())
            val receiveResult = filePasswordExchange.receive()

            expectThat(sendResult.failure).isFalse()
            expectThat(receiveResult.failure).isFalse()
            expectThat(normalizePasswordInfoMap(receiveResult.getOrNull()!!)) isEqualTo fixture
        } finally {
            homeDirectory.toFile().deleteRecursively()
        }
    }

    @Provide
    fun fixtures() = exchangeFixtures()
}
