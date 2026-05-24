package de.pflugradts.passbird.adapter.exchange

import de.pflugradts.passbird.PROPERTY
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.property.ExchangeFixture
import de.pflugradts.passbird.property.exchangeFixtures
import de.pflugradts.passbird.property.normalizePasswordInfoMap
import de.pflugradts.passbird.property.orThrow
import de.pflugradts.passbird.property.toPasswordInfoMap
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.Report
import net.jqwik.api.Reporting
import net.jqwik.api.Tag
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Files

@Tag(PROPERTY)
class FilePasswordExchangePropertyTest {

    @Property(tries = 25)
    @Report(Reporting.FALSIFIED)
    fun preservesExchangeDataAcrossExportAndImport(@ForAll("fixtures") fixture: ExchangeFixture) {
        val homeDirectory = Files.createTempDirectory("passbird-exchange-property")

        try {
            val filePasswordExchange = FilePasswordExchange(
                SystemOperation(),
                PassbirdRunContext(homeDirectory.toString().toDirectory(), Slot.DEFAULT),
            )

            filePasswordExchange.send(fixture.toPasswordInfoMap()).orThrow("password export")
            val received = filePasswordExchange.receive().orThrow("password import")

            expectThat(normalizePasswordInfoMap(received)) isEqualTo fixture
        } finally {
            homeDirectory.toFile().deleteRecursively()
        }
    }

    @Provide
    fun fixtures() = exchangeFixtures()
}
