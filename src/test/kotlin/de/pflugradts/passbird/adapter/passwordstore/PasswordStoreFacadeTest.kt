package de.pflugradts.passbird.adapter.passwordstore

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.nest.NestSlot.DEFAULT
import de.pflugradts.passbird.domain.model.nest.NestSlot.N1
import de.pflugradts.passbird.domain.model.nest.NestSlot.N2
import de.pflugradts.passbird.domain.model.nest.NestSlot.N3
import de.pflugradts.passbird.domain.model.nest.NestSlot.N4
import de.pflugradts.passbird.domain.model.nest.NestSlot.N5
import de.pflugradts.passbird.domain.model.nest.NestSlot.N6
import de.pflugradts.passbird.domain.model.nest.NestSlot.N7
import de.pflugradts.passbird.domain.model.nest.NestSlot.N8
import de.pflugradts.passbird.domain.model.nest.NestSlot.N9
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
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
class PasswordStoreFacadeTest {

    private val configuration = mockk<Configuration>()
    private val cryptoProvider = mockk<CryptoProvider>()
    private val nestService = createNestServiceForTesting()
    private val systemOperation = spyk(SystemOperation())
    private var passwordStoreFacade: PasswordStoreFacade = PasswordStoreFacade(
        passwordStoreReader = PasswordStoreReader(
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            nestService = nestService,
            systemOperation = systemOperation,
        ),
        passwordStoreWriter = PasswordStoreWriter(
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            nestService = nestService,
            systemOperation = systemOperation,
        ),
    )

    private var tempPasswordStoreDirectory = UUID.randomUUID().toString()
    private var databaseFilename = tempPasswordStoreDirectory + File.separator + ReadableConfiguration.DATABASE_FILENAME

    @BeforeEach
    fun setup() {
        expectThat(File(tempPasswordStoreDirectory).mkdir()).isTrue()
        fakeConfiguration(instance = configuration, withPasswordStoreLocation = tempPasswordStoreDirectory)
        every { cryptoProvider.encrypt(any(Shell::class)) } answers { firstArg() }
        every { cryptoProvider.decrypt(any(Shell::class)) } answers { firstArg() }
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(databaseFilename).delete()).isTrue()
        expectThat(File(tempPasswordStoreDirectory).delete()).isTrue()
    }

    @Test
    fun `should write to and them read from database`() {
        // given
        val eggs = someEggs()

        // when
        passwordStoreFacade.sync { eggs.stream() }
        expectThat(File(databaseFilename)).exists()
        val actual = passwordStoreFacade.restore()

        // then
        expectThat(actual.get().toList()) containsExactly eggs
    }

    @Test
    fun `should write to and them read from database using nests`() {
        // given
        val nest1 = shellOf("nest1")
        val nest3 = shellOf("Nest3")
        val nest9 = shellOf("+neSt*9")
        nestService.place(nest1, N1)
        nestService.place(nest3, N3)
        nestService.place(nest9, N9)
        val egg1 = createEggForTesting(
            withEggIdShell = shellOf("EggId1"),
            withPasswordShell = shellOf("Password1"),
            withNestSlot = DEFAULT,
        )
        val egg2 = createEggForTesting(
            withEggIdShell = shellOf("EggId2"),
            withPasswordShell = shellOf("Password2"),
            withNestSlot = N1,
        )
        val egg3 = createEggForTesting(
            withEggIdShell = shellOf("EggId3"),
            withPasswordShell = shellOf("Password3"),
            withNestSlot = N3,
        )
        val egg3b = createEggForTesting(
            withEggIdShell = shellOf("EggId3"),
            withPasswordShell = shellOf("Password3b"),
            withNestSlot = N9,
        )
        val eggs = listOf(egg1, egg2, egg3, egg3b)

        // when
        passwordStoreFacade.sync { eggs.stream() }
        nestService.populate(Collections.nCopies(CAPACITY, emptyShell()))
        expectThat(File(databaseFilename)).exists()
        val actual = passwordStoreFacade.restore()

        // then
        expectThat(actual.get().toList()) containsExactly eggs
        listOf(N2, N4, N5, N6, N7, N8).forEach { expectThat(nestService.atNestSlot(it).isPresent).isFalse() }
        mapOf(
            N1 to nest1,
            N3 to nest3,
            N9 to nest9,
        ).forEach { (k, v) ->
            expectThat(nestService.atNestSlot(k).isPresent)
            expectThat(nestService.atNestSlot(k).get().shell) isEqualTo v
        }
    }

    @Test
    fun `should write to and them read from empty database`() {
        // given
        expectThat(File(databaseFilename).createNewFile()).isTrue()

        // when
        val actual = passwordStoreFacade.restore()

        // then
        expectThat(actual.get().count()) isEqualTo 0
    }

    @Test
    fun `should create empty password database if file not exists`() {
        // given
        expectThat(File(databaseFilename).createNewFile()).isTrue()

        // when
        val actual = passwordStoreFacade.restore()

        // then
        expectThat(actual.get().count()) isEqualTo 0
    }

    @Test
    fun `should create empty password database if file is empty`() {
        // given
        expectThat(File(databaseFilename).createNewFile()).isTrue()

        // when
        val actual = passwordStoreFacade.restore()

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
            fakeConfiguration(instance = configuration, withPasswordStoreLocation = tempPasswordStoreDirectory, withVerifySignature = true)

            mockkStatic(::signature)
            every { signature() } returns manipulatedSignature
            passwordStoreFacade.sync { eggs.stream() }
            expectThat(File(databaseFilename)).exists()
            unmockkAll()

            val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

            // when
            captureSystemErr.during {
                passwordStoreFacade.restore()
            }
            val actual = captureSystemErr.capture

            // then
            expectThat(actual) contains "Signature of password database could not be verified."
            expectThat(actual) contains "Shutting down due to signature failure."
            verify(exactly = 1) { systemOperation.exit() }
        }

        @Test
        fun `should report failure on invalid signature with verifySignature set to false`() {
            // given
            val eggs = someEggs()
            val manipulatedSignature = signature().reversedArray()
            every { systemOperation.exit() } returns Unit
            fakeConfiguration(instance = configuration, withPasswordStoreLocation = tempPasswordStoreDirectory, withVerifySignature = false)

            mockkStatic(::signature)
            every { signature() } returns manipulatedSignature
            passwordStoreFacade.sync { eggs.stream() }
            expectThat(File(databaseFilename)).exists()
            unmockkAll()

            val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

            // when
            var restored = 0
            captureSystemErr.during {
                restored = passwordStoreFacade.restore().get().count().toInt()
            }
            val actual = captureSystemErr.capture

            // then
            expectThat(restored) isEqualTo eggs.size
            expectThat(actual) contains "Signature of password database could not be verified."
            expectThat(actual.contains("Shutting down due to signature failure.")).isFalse()
            verify(exactly = 0) { systemOperation.exit() }
        }

        @Test
        fun `should shut down on invalid checksum with verifyChecksum set to true`() {
            // given
            val eggs = someEggs()
            every { systemOperation.exit() } returns Unit
            fakeConfiguration(instance = configuration, withPasswordStoreLocation = tempPasswordStoreDirectory, withVerifyChecksum = true)

            mockkStatic(::checksum)
            every { checksum(any()) } returns 0
            passwordStoreFacade.sync { eggs.stream() }
            expectThat(File(databaseFilename)).exists()
            unmockkAll()

            val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

            // when
            captureSystemErr.during {
                passwordStoreFacade.restore()
            }
            val actual = captureSystemErr.capture

            // then
            expectThat(actual) contains "Checksum of password database could not be verified."
            expectThat(actual) contains "Shutting down due to checksum failure."
            verify(exactly = 1) { systemOperation.exit() }
        }

        @Test
        fun `should report failure on invalid checksum with verifyChecksum set to false`() {
            // given
            val eggs = someEggs()
            every { systemOperation.exit() } returns Unit
            fakeConfiguration(instance = configuration, withPasswordStoreLocation = tempPasswordStoreDirectory, withVerifyChecksum = false)

            mockkStatic(::checksum)
            every { checksum(any()) } returns 0
            passwordStoreFacade.sync { eggs.stream() }
            expectThat(File(databaseFilename)).exists()
            unmockkAll()

            val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

            // when
            var restored = 0
            captureSystemErr.during {
                restored = passwordStoreFacade.restore().get().count().toInt()
            }
            val actual = captureSystemErr.capture

            // then
            expectThat(restored) isEqualTo eggs.size
            expectThat(actual) contains "Checksum of password database could not be verified."
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
