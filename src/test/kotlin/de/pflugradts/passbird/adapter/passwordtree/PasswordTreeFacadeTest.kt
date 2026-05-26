package de.pflugradts.passbird.adapter.passwordtree

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.passwordtree.checksum
import de.pflugradts.passbird.application.passwordtree.checksumBytes
import de.pflugradts.passbird.application.passwordtree.signature
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.application.util.posixPermissionsIfSupported
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
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
import de.pflugradts.passbird.domain.service.password.tree.EggStreamSupplier
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
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import strikt.java.exists
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import java.util.Collections

@Tag(INTEGRATION)
class PasswordTreeFacadeTest {

    private val configuration = mockk<Configuration>()
    private val cryptoProvider = createAesGcmCipherForTesting()
    private val nestService = createNestServiceForTesting()
    private val systemOperation = spyk(SystemOperation())
    private val passwordTreeEnvelope = PasswordTreeEnvelope()
    private val passwordTreePayloadWriter = PasswordTreePayloadWriter()
    private var passwordTreeFacade: PasswordTreeFacade = PasswordTreeFacade(
        passwordTreeReader = PasswordTreeReader(
            systemOperation = systemOperation,
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadReader = PasswordTreePayloadReader(configuration, systemOperation),
        ),
        passwordTreeWriter = PasswordTreeWriter(
            systemOperation = systemOperation,
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadWriter = passwordTreePayloadWriter,
        ),
    )

    private var tempPasswordTreeDirectory = Files.createTempDirectory("passbird-password-tree").toString()
    private var passwordTreeFilename = tempPasswordTreeDirectory + File.separator + ReadableConfiguration.PASSWORD_TREE_FILENAME

    @BeforeEach
    fun setup() {
        fakeConfiguration(instance = configuration, withPasswordTreeLocation = tempPasswordTreeDirectory)
        every { systemOperation.exit() } returns Unit
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(tempPasswordTreeDirectory).deleteRecursively()).isTrue()
    }

    @Test
    fun `should write to and then read from tree`() {
        // given
        val eggs = someEggs()

        // when
        passwordTreeFacade.sync(EggStreamSupplier({ eggs.stream()}))
        expectThat(File(passwordTreeFilename)).exists()
        posixPermissionsIfSupported(Paths.get(passwordTreeFilename))?.let {
            expectThat(it) isEqualTo setOf(OWNER_READ, OWNER_WRITE)
        }
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
        val egg1 = createEggFromStrings(slot = DEFAULT, eggId = "EggId1", password = "Password1")
        val egg2 = createEggFromStrings(slot = S1, eggId = "EggId2", password = "Password2")
        val egg3a = createEggFromStrings(slot = S3, eggId = "EggId3", password = "Password3")
        val egg3b = createEggFromStrings(slot = S9, eggId = "EggId3", password = "Password3")
        val eggs = listOf(egg1, egg2, egg3a, egg3b)

        // when
        passwordTreeFacade.sync(EggStreamSupplier({ eggs.stream() }, nests = nestService.snapshot()))
        expectThat(File(passwordTreeFilename)).exists()
        val actual = passwordTreeFacade.restore()

        // then
        expectThat(actual.get().toList()) containsExactly eggs
        listOf(S2, S4, S5, S6, S7, S8).forEach { expectThat(actual.nests()[it.index() - 1].isEmpty).isTrue() }
        mapOf(
            S1 to nest1,
            S3 to nest3,
            S9 to nest9,
        ).forEach { (k, v) ->
            expectThat(actual.nests()[k.index() - 1]) isEqualTo v
        }
    }

    @Test
    fun `should create empty tree if file does not exist without reporting a failure`() {
        // given
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        // when
        val actual = captureSystemErr.during { passwordTreeFacade.restore() }

        // then
        expectThat(actual.get().count()) isEqualTo 0
        expectThat(captureSystemErr.capture).isEqualTo("")
    }

    @Test
    fun `should create empty tree if file is empty without reporting a failure`() {
        // given
        expectThat(File(passwordTreeFilename).createNewFile()).isTrue()
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        // when
        val actual = captureSystemErr.during { passwordTreeFacade.restore() }

        // then
        expectThat(actual.get().count()) isEqualTo 0
        expectThat(captureSystemErr.capture).isEqualTo("")
    }

    @Test
    fun `should return sync failure when writing password tree fails`() {
        // given
        val failingSystemOperation = mockk<SystemOperation>()
        fakeSystemOperation(instance = failingSystemOperation)
        every {
            failingSystemOperation.resolvePath(
                any(de.pflugradts.passbird.application.Directory::class),
                any(de.pflugradts.passbird.application.FileName::class),
            )
        } returns Paths.get(passwordTreeFilename)
        every { failingSystemOperation.writeBytesToSensitiveFile(any(), any()) } throws IOException("disk full")
        val failingPasswordTreeFacade = PasswordTreeFacade(
            passwordTreeReader = PasswordTreeReader(
                systemOperation = failingSystemOperation,
                configuration = configuration,
                cryptoProvider = cryptoProvider,
                passwordTreeEnvelope = passwordTreeEnvelope,
                passwordTreePayloadReader = PasswordTreePayloadReader(configuration, failingSystemOperation),
            ),
            passwordTreeWriter = PasswordTreeWriter(
                systemOperation = failingSystemOperation,
                configuration = configuration,
                cryptoProvider = cryptoProvider,
                passwordTreeEnvelope = passwordTreeEnvelope,
                passwordTreePayloadWriter = passwordTreePayloadWriter,
            ),
        )
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        // when
        val actual = captureSystemErr.during { failingPasswordTreeFacade.sync(EggStreamSupplier({ someEggs().stream() })) }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isNotNull().isA<IOException>()
        expectThat(captureSystemErr.capture) contains "Password Tree could not be synced: disk full"
    }

    @Test
    fun `should shut down on decrypt failure instead of falling back to empty tree`() {
        // given
        every { systemOperation.exit() } returns Unit
        File(passwordTreeFilename).writeText("not an encrypted password tree")
        expectThat(File(passwordTreeFilename)).exists()
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        // when
        val actual = captureSystemErr.during {
            tryCatching { passwordTreeFacade.restore() }
        }

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isA<Exception>()
        expectThat(captureSystemErr.capture) contains "Password Tree at 'passbird.tree' could not be decrypted:"
        verify(exactly = 1) { systemOperation.exit() }
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
            passwordTreeFacade.sync(EggStreamSupplier({ eggs.stream()}))
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
            passwordTreeFacade.sync(EggStreamSupplier({ eggs.stream()}))
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
            passwordTreeFacade.sync(EggStreamSupplier({ eggs.stream()}))
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
            passwordTreeFacade.sync(EggStreamSupplier({ eggs.stream()}))
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

        @Test
        fun `should shut down when the final password tree content byte is corrupted`() {
            // given
            val eggs = someEggs()
            every { systemOperation.exit() } returns Unit
            fakeConfiguration(instance = configuration, withPasswordTreeLocation = tempPasswordTreeDirectory, withVerifyChecksum = true)
            passwordTreeFacade.sync(EggStreamSupplier({ eggs.stream() }))
            expectThat(File(passwordTreeFilename)).exists()
            val decryptedTree = cryptoProvider.decrypt(
                encryptedShellOf(passwordTreeEnvelope.unwrap(File(passwordTreeFilename).readBytes())),
            ).toByteArray()
            decryptedTree[decryptedTree.size - checksumBytes() - 1] = (decryptedTree[decryptedTree.size - checksumBytes() - 1] + 1).toByte()
            File(passwordTreeFilename).writeBytes(passwordTreeEnvelope.wrap(cryptoProvider.encrypt(shellOf(decryptedTree)).toByteArray()))
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
    }

    private fun someEggs() = listOf(
        createEggFromStrings(eggId = "EggId1", password = "Password1"),
        createEggFromStrings(eggId = "EggId2", password = "Password2"),
        createEggFromStrings(eggId = "EggId3", password = "Password3"),
    )

    private fun createEggFromStrings(slot: Slot = DEFAULT, eggId: String, password: String) = createEgg(
        slot = slot,
        eggIdShell = cryptoProvider.encrypt(shellOf(eggId)),
        passwordShell = cryptoProvider.encrypt(shellOf(password)),
    )
}
