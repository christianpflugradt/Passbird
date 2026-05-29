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
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Files

@Tag(PROPERTY)
class FilePasswordExchangePropertyTest {

    @Test
    fun preservesExchangeDataAcrossExportAndImport() {
        runBlocking {
            checkAll(25, exchangeFixtures()) { fixture ->
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
        }
    }
}
