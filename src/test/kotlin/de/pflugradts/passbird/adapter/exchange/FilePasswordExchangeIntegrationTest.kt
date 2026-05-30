package de.pflugradts.passbird.adapter.exchange

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.PasswordInfo
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.posixPermissionsIfSupported
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.containsKey
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE

@Tag(INTEGRATION)
class FilePasswordExchangeIntegrationTest {

    private val tempExchangeDirectory = Files.createTempDirectory("passbird-exchange-integration").toString()
    private val exchangeFile = tempExchangeDirectory + File.separator + ReadableConfiguration.EXCHANGE_FILENAME
    private val filePasswordExchange = FilePasswordExchange(
        SystemOperation(),
        PassbirdRunContext(tempExchangeDirectory.toDirectory(), Slot.DEFAULT),
    )

    @AfterEach
    fun cleanup() {
        expectThat(File(tempExchangeDirectory).deleteRecursively()).isTrue()
    }

    @Test
    fun `should export and re-import passwords across multiple nests`() {
        // given
        val givenEgg1 = PasswordInfo(
            ShellPair(shellOf("EggId1"), shellOf("Password1")),
            proteinShellPairs(
                Slot.DEFAULT to ShellPair(shellOf("type0"), shellOf("structure0")),
                Slot.S3 to ShellPair(shellOf("type3"), shellOf("structure3")),
            ),
        )
        val givenEgg2 = PasswordInfo(ShellPair(shellOf("EggId2"), shellOf("Password2")), proteinShellPairs())
        val givenEgg3 = PasswordInfo(ShellPair(shellOf("EggId3"), shellOf("Password3")), proteinShellPairs())
        val givenEgg4 = PasswordInfo(
            ShellPair(shellOf("EggId4"), shellOf("Password4")),
            proteinShellPairs(Slot.S9 to ShellPair(shellOf("type9"), shellOf("structure9"))),
        )
        val givenEgg5 = PasswordInfo(ShellPair(shellOf("EggId5"), shellOf("Password5")), proteinShellPairs())

        // whe
        filePasswordExchange.send(
            mapOf(
                Slot.DEFAULT.toNest() to listOf(givenEgg1, givenEgg2),
                Slot.S2.toNest() to listOf(givenEgg3),
                Slot.S9.toNest() to listOf(givenEgg4, givenEgg5),
            ),
        ).getOrNull()
        posixPermissionsIfSupported(Paths.get(exchangeFile))?.let {
            expectThat(it) isEqualTo setOf(OWNER_READ, OWNER_WRITE)
        }
        val actual = filePasswordExchange.receive().getOrNull()!!

        // then
        expectThat(actual) hasSize 3 containsKey Slot.DEFAULT.toNest() containsKey Slot.S2.toNest() containsKey Slot.S9.toNest()
        expectThat(actual[Slot.DEFAULT.toNest()]!!).containsExactlyInAnyOrder(givenEgg1, givenEgg2)
        expectThat(actual[Slot.S2.toNest()]!!).containsExactlyInAnyOrder(givenEgg3)
        expectThat(actual[Slot.S9.toNest()]!!).containsExactlyInAnyOrder(givenEgg4, givenEgg5)
    }

    @Test
    fun `should receive proteins according to explicit slot values when json is sparse and reordered`() {
        // given
        writeExchangeFile(
            """
            {
              "exportedContent": [
                {
                  "exportedNest": {
                    "nestId": "DEFAULT",
                    "slot": 0
                  },
                  "exportedEggs": [
                    {
                      "eggId": "EggId1",
                      "password": "Password1",
                      "proteins": [
                        {
                          "proteinType": "type9",
                          "proteinStructure": "structure9",
                          "slot": 9
                        },
                        {
                          "proteinType": "type2",
                          "proteinStructure": "structure2",
                          "slot": 2
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """,
        )

        // when
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual.failure).isFalse()
        expectThat(actual.getOrNull()!![Slot.DEFAULT.toNest()]!!.single().second).isEqualTo(
            proteinShellPairs(
                Slot.S2 to ShellPair(shellOf("type2"), shellOf("structure2")),
                Slot.S9 to ShellPair(shellOf("type9"), shellOf("structure9")),
            ),
        )
    }

    @Test
    fun `should fail receive when protein slot values are duplicated`() {
        // given
        writeExchangeFile(
            """
            {
              "exportedContent": [
                {
                  "exportedNest": {
                    "nestId": "DEFAULT",
                    "slot": 0
                  },
                  "exportedEggs": [
                    {
                      "eggId": "EggId1",
                      "password": "Password1",
                      "proteins": [
                        {
                          "proteinType": "type1",
                          "proteinStructure": "structure1",
                          "slot": 1
                        },
                        {
                          "proteinType": "type1b",
                          "proteinStructure": "structure1b",
                          "slot": 1
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """,
        )

        // when
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual.failure).isTrue()
    }

    @Test
    fun `should fail receive when egg ids are duplicated within one nest`() {
        // given
        writeExchangeFile(
            """
            {
              "exportedContent": [
                {
                  "exportedNest": {
                    "nestId": "DEFAULT",
                    "slot": 0
                  },
                  "exportedEggs": [
                    {
                      "eggId": "EggId1",
                      "password": "Password1",
                      "proteins": []
                    },
                    {
                      "eggId": "EggId1",
                      "password": "Password2",
                      "proteins": []
                    }
                  ]
                }
              ]
            }
            """,
        )

        // when
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<IllegalArgumentException>()
    }

    @Test
    fun `should fail receive when protein slot values are out of range`() {
        // given
        writeExchangeFile(
            """
            {
              "exportedContent": [
                {
                  "exportedNest": {
                    "nestId": "DEFAULT",
                    "slot": 0
                  },
                  "exportedEggs": [
                    {
                      "eggId": "EggId1",
                      "password": "Password1",
                      "proteins": [
                        {
                          "proteinType": "type10",
                          "proteinStructure": "structure10",
                          "slot": 10
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """,
        )

        // when
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual.failure).isTrue()
    }

    @Test
    fun `should fail receive when nest slot value is missing`() {
        // given
        writeExchangeFile(
            """
            {
              "exportedContent": [
                {
                  "exportedNest": {
                    "nestId": "DEFAULT"
                  },
                  "exportedEggs": [
                    {
                      "eggId": "EggId1",
                      "password": "Password1",
                      "proteins": []
                    }
                  ]
                }
              ]
            }
            """,
        )

        // when
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<IllegalArgumentException>()
    }

    @ParameterizedTest
    @ValueSource(strings = ["\"\"", "\"   \"", "\"\\t\"", "\"\\n\""])
    fun `should fail receive when custom nest id is empty or blank`(givenNestIdJson: String) {
        // given
        writeExchangeFile(
            """
            {
              "exportedContent": [
                {
                  "exportedNest": {
                    "nestId": $givenNestIdJson,
                    "slot": 2
                  },
                  "exportedEggs": [
                    {
                      "eggId": "EggId1",
                      "password": "Password1",
                      "proteins": []
                    }
                  ]
                }
              ]
            }
            """,
        )

        // when
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<IllegalArgumentException>()
    }

    @Test
    fun `should fail receive when nest slot value is duplicated`() {
        // given
        writeExchangeFile(
            """
            {
              "exportedContent": [
                {
                  "exportedNest": {
                    "nestId": "Default",
                    "slot": 0
                  },
                  "exportedEggs": [
                    {
                      "eggId": "EggId1",
                      "password": "Password1",
                      "proteins": []
                    }
                  ]
                },
                {
                  "exportedNest": {
                    "nestId": "Other",
                    "slot": 0
                  },
                  "exportedEggs": [
                    {
                      "eggId": "EggId2",
                      "password": "Password2",
                      "proteins": []
                    }
                  ]
                }
              ]
            }
            """,
        )

        // when
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<IllegalArgumentException>()
    }

    @Test
    fun `should fail receive when nest slot value is out of range`() {
        // given
        writeExchangeFile(
            """
            {
              "exportedContent": [
                {
                  "exportedNest": {
                    "nestId": "DEFAULT",
                    "slot": 10
                  },
                  "exportedEggs": [
                    {
                      "eggId": "EggId1",
                      "password": "Password1",
                      "proteins": []
                    }
                  ]
                }
              ]
            }
            """,
        )

        // when
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<IllegalArgumentException>()
    }

    private fun writeExchangeFile(content: String) {
        File(exchangeFile).writeText(content.trimIndent())
    }
}

private fun Slot.toNest() = createNest(shellOf(this.name), this)

private fun proteinShellPairs(vararg proteins: Pair<Slot, ShellPair>) = MutableList(Slot.entries.size) {
    ShellPair(emptyShell(), emptyShell())
}.apply {
    proteins.forEach { (slot, shells) -> this[slot.index()] = shells }
}
