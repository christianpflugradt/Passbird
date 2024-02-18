package de.pflugradts.passbird.adapter.passwordtree

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.slot.Slot.S3
import de.pflugradts.passbird.domain.model.slot.Slot.S4
import de.pflugradts.passbird.domain.model.slot.Slot.S5
import de.pflugradts.passbird.domain.model.slot.Slot.S6
import de.pflugradts.passbird.domain.model.slot.Slot.S7
import de.pflugradts.passbird.domain.model.slot.Slot.S8
import de.pflugradts.passbird.domain.model.slot.Slot.S9
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import strikt.java.exists
import java.io.File
import java.util.Collections
import java.util.UUID

@Tag(INTEGRATION)
class PasswordTreeFacadeTest {

    private val configuration = mockk<Configuration>()
    private val cryptoProvider = mockk<CryptoProvider>()
    private val nestService = createNestServiceForTesting()
    private val systemOperation = spyk(SystemOperation())
    private var passwordTreeFacade: PasswordTreeFacade = PasswordTreeFacade(
        passwordTreeReader = PasswordTreeReader(
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            nestService = nestService,
            systemOperation = systemOperation,
        ),
        passwordTreeWriter = PasswordTreeWriter(
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            nestService = nestService,
            systemOperation = systemOperation,
        ),
    )

    private var tempPasswordTreeDirectory = UUID.randomUUID().toString()
    private var passwordTreeFilename = tempPasswordTreeDirectory + File.separator + ReadableConfiguration.PASSWORD_TREE_FILENAME

    @BeforeEach
    fun setup() {
        expectThat(File(tempPasswordTreeDirectory).mkdir()).isTrue()
        fakeConfiguration(instance = configuration, withPasswordTreeLocation = tempPasswordTreeDirectory)
        every { cryptoProvider.encrypt(any(Shell::class)) } answers { firstArg() }
        every { cryptoProvider.decrypt(any(Shell::class)) } answers { firstArg() }
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(passwordTreeFilename).delete()).isTrue()
        expectThat(File(tempPasswordTreeDirectory).delete()).isTrue()
    }

    @Test
    fun `should write to and then read from tree`() {
        // given
        val eggs = someEggs()

        // when
        passwordTreeFacade.sync { eggs.stream() }
        expectThat(File(passwordTreeFilename)).exists()
        val actual = passwordTreeFacade.restore()

        // then
        expectThat(actual.get().toList()) containsExactly eggs
    }

    @Test
    fun `should write to and them read from tree using nests`() {
        // given
        val nest1 = shellOf("nest1")
        val nest3 = shellOf("Nest3")
        val nest9 = shellOf("+neSt*9")
        nestService.place(nest1, S1)
        nestService.place(nest3, S3)
        nestService.place(nest9, S9)
        val egg1 = createEggForTesting(
            withEggIdShell = shellOf("EggId1"),
            withPasswordShell = shellOf("Password1"),
            withSlot = DEFAULT,
        )
        val egg2 = createEggForTesting(
            withEggIdShell = shellOf("EggId2"),
            withPasswordShell = shellOf("Password2"),
            withSlot = S1,
        )
        val egg3 = createEggForTesting(
            withEggIdShell = shellOf("EggId3"),
            withPasswordShell = shellOf("Password3"),
            withSlot = S3,
        )
        val egg3b = createEggForTesting(
            withEggIdShell = shellOf("EggId3"),
            withPasswordShell = shellOf("Password3b"),
            withSlot = S9,
        )
        val eggs = listOf(egg1, egg2, egg3, egg3b)

        // when
        passwordTreeFacade.sync { eggs.stream() }
        nestService.populate(Collections.nCopies(CAPACITY, emptyShell()))
        expectThat(File(passwordTreeFilename)).exists()
        val actual = passwordTreeFacade.restore()

        // then
        expectThat(actual.get().toList()) containsExactly eggs
        listOf(S2, S4, S5, S6, S7, S8).forEach { expectThat(nestService.atNestSlot(it).isPresent).isFalse() }
        mapOf(
            S1 to nest1,
            S3 to nest3,
            S9 to nest9,
        ).forEach { (k, v) ->
            expectThat(nestService.atNestSlot(k).isPresent)
            expectThat(nestService.atNestSlot(k).get().viewNestId()) isEqualTo v
        }
    }

    @Test
    fun `should write to and them read from empty tree`() {
        // given
        expectThat(File(passwordTreeFilename).createNewFile()).isTrue()

        // when
        val actual = passwordTreeFacade.restore()

        // then
        expectThat(actual.get().count()) isEqualTo 0
    }

    @Test
    fun `should create empty tree if file not exists`() {
        // given
        expectThat(File(passwordTreeFilename).createNewFile()).isTrue()

        // when
        val actual = passwordTreeFacade.restore()

        // then
        expectThat(actual.get().count()) isEqualTo 0
    }

    @Test
    fun `should create empty tree if file is empty`() {
        // given
        expectThat(File(passwordTreeFilename).createNewFile()).isTrue()

        // when
        val actual = passwordTreeFacade.restore()

        // then
        expectThat(actual.get().count()) isEqualTo 0
    }

    @Nested
    inner class SignatureAndCheckSumFailureTest {

        @Test
        fun `should shut down on invalid signature with verifySignature set to true`() {
            // given
            val eggs = someEggs()
            val manipulatedSignature = signature().reversedArray()
            every { systemOperation.exit() } returns Unit
            fakeConfiguration(instance = configuration, withPasswordTreeLocation = tempPasswordTreeDirectory, withVerifySignature = true)

            mockkStatic(::signature)
            every { signature() } returns manipulatedSignature
            passwordTreeFacade.sync { eggs.stream() }
            expectThat(File(passwordTreeFilename)).exists()
            unmockkAll()

            val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

            // when
            captureSystemErr.during {
                passwordTreeFacade.restore()
            }
            val actual = captureSystemErr.capture

            // then
            expectThat(actual) contains "Signature of Password Tree could not be verified."
            expectThat(actual) contains "Shutting down due to signature failure."
            verify(exactly = 1) { systemOperation.exit() }
        }

        @Test
        fun `should report failure on invalid signature with verifySignature set to false`() {
            // given
            val eggs = someEggs()
            val manipulatedSignature = signature().reversedArray()
            every { systemOperation.exit() } returns Unit
            fakeConfiguration(instance = configuration, withPasswordTreeLocation = tempPasswordTreeDirectory, withVerifySignature = false)

            mockkStatic(::signature)
            every { signature() } returns manipulatedSignature
            passwordTreeFacade.sync { eggs.stream() }
            expectThat(File(passwordTreeFilename)).exists()
            unmockkAll()

            val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

            // when
            var restored = 0
            captureSystemErr.during {
                restored = passwordTreeFacade.restore().get().count().toInt()
            }
            val actual = captureSystemErr.capture

            // then
            expectThat(restored) isEqualTo eggs.size
            expectThat(actual) contains "Signature of Password Tree could not be verified."
            expectThat(actual.contains("Shutting down due to signature failure.")).isFalse()
            verify(exactly = 0) { systemOperation.exit() }
        }

        @Test
        fun `should shut down on invalid checksum with verifyChecksum set to true`() {
            // given
            val eggs = someEggs()
            every { systemOperation.exit() } returns Unit
            fakeConfiguration(instance = configuration, withPasswordTreeLocation = tempPasswordTreeDirectory, withVerifyChecksum = true)

            mockkStatic(::checksum)
            every { checksum(any()) } returns 0
            passwordTreeFacade.sync { eggs.stream() }
            expectThat(File(passwordTreeFilename)).exists()
            unmockkAll()

            val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

            // when
            captureSystemErr.during {
                passwordTreeFacade.restore()
            }
            val actual = captureSystemErr.capture

            // then
            expectThat(actual) contains "Checksum of Password Tree could not be verified."
            expectThat(actual) contains "Shutting down due to checksum failure."
            verify(exactly = 1) { systemOperation.exit() }
        }

        @Test
        fun `should report failure on invalid checksum with verifyChecksum set to false`() {
            // given
            val eggs = someEggs()
            every { systemOperation.exit() } returns Unit
            fakeConfiguration(instance = configuration, withPasswordTreeLocation = tempPasswordTreeDirectory, withVerifyChecksum = false)

            mockkStatic(::checksum)
            every { checksum(any()) } returns 0
            passwordTreeFacade.sync { eggs.stream() }
            expectThat(File(passwordTreeFilename)).exists()
            unmockkAll()

            val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

            // when
            var restored = 0
            captureSystemErr.during {
                restored = passwordTreeFacade.restore().get().count().toInt()
            }
            val actual = captureSystemErr.capture

            // then
            expectThat(restored) isEqualTo eggs.size
            expectThat(actual) contains "Checksum of Password Tree could not be verified."
            expectThat(actual.contains("Shutting down due to checksum failure.")).isFalse()
            verify(exactly = 0) { systemOperation.exit() }
        }
    }

    private fun someEggs() = listOf(
        createEggForTesting(withEggIdShell = shellOf("EggId1"), withPasswordShell = shellOf("Password1")),
        createEggForTesting(withEggIdShell = shellOf("EggId2"), withPasswordShell = shellOf("Password2")),
        createEggForTesting(withEggIdShell = shellOf("EggId3"), withPasswordShell = shellOf("Password3")),
    )
}
