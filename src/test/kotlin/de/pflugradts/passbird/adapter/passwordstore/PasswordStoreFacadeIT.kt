package de.pflugradts.passbird.adapter.passwordstore

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.nest.Slot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.nest.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Slot.N1
import de.pflugradts.passbird.domain.model.nest.Slot.N2
import de.pflugradts.passbird.domain.model.nest.Slot.N3
import de.pflugradts.passbird.domain.model.nest.Slot.N4
import de.pflugradts.passbird.domain.model.nest.Slot.N5
import de.pflugradts.passbird.domain.model.nest.Slot.N6
import de.pflugradts.passbird.domain.model.nest.Slot.N7
import de.pflugradts.passbird.domain.model.nest.Slot.N8
import de.pflugradts.passbird.domain.model.nest.Slot.N9
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.service.createNestServiceForTesting
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

class PasswordStoreFacadeIT {

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
        every { cryptoProvider.encrypt(any(Bytes::class)) } answers { firstArg() }
        every { cryptoProvider.decrypt(any(Bytes::class)) } answers { firstArg() }
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(databaseFilename).delete()).isTrue()
        expectThat(File(tempPasswordStoreDirectory).delete()).isTrue()
    }

    @Test
    fun `should write to and them read from database`() {
        // given
        val passwordEntries = somePasswordEntries()

        // when
        passwordStoreFacade.sync { passwordEntries.stream() }
        expectThat(File(databaseFilename)).exists()
        val actual = passwordStoreFacade.restore()

        // then
        expectThat(actual.get().toList()) containsExactly passwordEntries
    }

    @Test
    fun `should write to and them read from database using nests`() {
        // given
        val nest1 = bytesOf("nest1")
        val nest3 = bytesOf("Nest3")
        val nest9 = bytesOf("+neSt*9")
        nestService.deploy(nest1, N1)
        nestService.deploy(nest3, N3)
        nestService.deploy(nest9, N9)
        val passwordEntry1 = createPasswordEntryForTesting(
            withKeyBytes = bytesOf("key1"),
            withPasswordBytes = bytesOf("password1"),
            withNestSlot = DEFAULT,
        )
        val passwordEntry2 = createPasswordEntryForTesting(
            withKeyBytes = bytesOf("key2"),
            withPasswordBytes = bytesOf("password2"),
            withNestSlot = N1,
        )
        val passwordEntry3 = createPasswordEntryForTesting(
            withKeyBytes = bytesOf("key3"),
            withPasswordBytes = bytesOf("password3"),
            withNestSlot = N3,
        )
        val passwordEntry3b = createPasswordEntryForTesting(
            withKeyBytes = bytesOf("key3"),
            withPasswordBytes = bytesOf("password3b"),
            withNestSlot = N9,
        )
        val passwordEntries = listOf(passwordEntry1, passwordEntry2, passwordEntry3, passwordEntry3b)

        // when
        passwordStoreFacade.sync { passwordEntries.stream() }
        nestService.populate(Collections.nCopies(CAPACITY, emptyBytes()))
        expectThat(File(databaseFilename)).exists()
        val actual = passwordStoreFacade.restore()

        // then
        expectThat(actual.get().toList()) containsExactly passwordEntries
        listOf(N2, N4, N5, N6, N7, N8).forEach { expectThat(nestService.atSlot(it).isPresent).isFalse() }
        mapOf(
            N1 to nest1,
            N3 to nest3,
            N9 to nest9,
        ).forEach { (k, v) ->
            expectThat(nestService.atSlot(k).isPresent)
            expectThat(nestService.atSlot(k).get().bytes) isEqualTo v
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
            val passwordEntries = somePasswordEntries()
            val manipulatedSignature = signature().reversedArray()
            every { systemOperation.exit() } returns Unit
            fakeConfiguration(instance = configuration, withPasswordStoreLocation = tempPasswordStoreDirectory, withVerifySignature = true)

            mockkStatic(::signature)
            every { signature() } returns manipulatedSignature
            passwordStoreFacade.sync { passwordEntries.stream() }
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
            val passwordEntries = somePasswordEntries()
            val manipulatedSignature = signature().reversedArray()
            every { systemOperation.exit() } returns Unit
            fakeConfiguration(instance = configuration, withPasswordStoreLocation = tempPasswordStoreDirectory, withVerifySignature = false)

            mockkStatic(::signature)
            every { signature() } returns manipulatedSignature
            passwordStoreFacade.sync { passwordEntries.stream() }
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
            expectThat(restored) isEqualTo passwordEntries.size
            expectThat(actual) contains "Signature of password database could not be verified."
            expectThat(actual.contains("Shutting down due to signature failure.")).isFalse()
            verify(exactly = 0) { systemOperation.exit() }
        }

        @Test
        fun `should shut down on invalid checksum with verifyChecksum set to true`() {
            // given
            val passwordEntries = somePasswordEntries()
            every { systemOperation.exit() } returns Unit
            fakeConfiguration(instance = configuration, withPasswordStoreLocation = tempPasswordStoreDirectory, withVerifyChecksum = true)

            mockkStatic(::checksum)
            every { checksum(any()) } returns 0
            passwordStoreFacade.sync { passwordEntries.stream() }
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
            val passwordEntries = somePasswordEntries()
            every { systemOperation.exit() } returns Unit
            fakeConfiguration(instance = configuration, withPasswordStoreLocation = tempPasswordStoreDirectory, withVerifyChecksum = false)

            mockkStatic(::checksum)
            every { checksum(any()) } returns 0
            passwordStoreFacade.sync { passwordEntries.stream() }
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
            expectThat(restored) isEqualTo passwordEntries.size
            expectThat(actual) contains "Checksum of password database could not be verified."
            expectThat(actual.contains("Shutting down due to checksum failure.")).isFalse()
            verify(exactly = 0) { systemOperation.exit() }
        }
    }

    private fun somePasswordEntries() = listOf(
        createPasswordEntryForTesting(withKeyBytes = bytesOf("key1"), withPasswordBytes = bytesOf("password1")),
        createPasswordEntryForTesting(withKeyBytes = bytesOf("key2"), withPasswordBytes = bytesOf("password2")),
        createPasswordEntryForTesting(withKeyBytes = bytesOf("key3"), withPasswordBytes = bytesOf("password3")),
    )
}
