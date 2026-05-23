package de.pflugradts.passbird.adapter.exchange

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.PasswordInfo
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.posixPermissionsIfSupported
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.containsKey
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import java.util.UUID

@Tag(INTEGRATION)
class FilePasswordExchangeIntegrationTest {

    private val tempExchangeDirectory = UUID.randomUUID().toString()
    private val exchangeFile = tempExchangeDirectory + File.separator + ReadableConfiguration.EXCHANGE_FILENAME
    private val filePasswordExchange = FilePasswordExchange(
        SystemOperation(),
        PassbirdRunContext(tempExchangeDirectory.toDirectory(), Slot.DEFAULT),
    )

    @BeforeEach
    fun setup() {
        expectThat(File(tempExchangeDirectory).mkdir()).isTrue()
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(exchangeFile).delete()).isTrue()
        expectThat(File(tempExchangeDirectory).delete()).isTrue()
    }

    @Test
    fun `should export and re-import passwords across multiple nests`() {
        // given
        val givenEgg1 = PasswordInfo(ShellPair(shellOf("EggId1"), shellOf("Password1")), emptyList())
        val givenEgg2 = PasswordInfo(ShellPair(shellOf("EggId2"), shellOf("Password2")), emptyList())
        val givenEgg3 = PasswordInfo(ShellPair(shellOf("EggId3"), shellOf("Password3")), emptyList())
        val givenEgg4 = PasswordInfo(ShellPair(shellOf("EggId4"), shellOf("Password4")), emptyList())
        val givenEgg5 = PasswordInfo(ShellPair(shellOf("EggId5"), shellOf("Password5")), emptyList())

        // whe
        filePasswordExchange.send(
            mapOf(
                Slot.DEFAULT.toNest() to listOf(givenEgg1, givenEgg2),
                Slot.S2.toNest() to listOf(givenEgg3),
                Slot.S9.toNest() to listOf(givenEgg4, givenEgg5),
            ),
        )
        posixPermissionsIfSupported(Paths.get(exchangeFile))?.let {
            expectThat(it) isEqualTo setOf(OWNER_READ, OWNER_WRITE)
        }
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual) hasSize 3 containsKey Slot.DEFAULT.toNest() containsKey Slot.S2.toNest() containsKey Slot.S9.toNest()
        expectThat(actual[Slot.DEFAULT.toNest()]!!).containsExactlyInAnyOrder(givenEgg1, givenEgg2)
        expectThat(actual[Slot.S2.toNest()]!!).containsExactlyInAnyOrder(givenEgg3)
        expectThat(actual[Slot.S9.toNest()]!!).containsExactlyInAnyOrder(givenEgg4, givenEgg5)
    }
}

private fun Slot.toNest() = createNest(shellOf(this.name), this)
