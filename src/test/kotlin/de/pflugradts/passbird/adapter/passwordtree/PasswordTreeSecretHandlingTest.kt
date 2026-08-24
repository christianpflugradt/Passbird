package de.pflugradts.passbird.adapter.passwordtree

import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.passwordtree.LegacyCurrentPasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.passwordtree.PasswordTreeSnapshot
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.service.password.tree.EggStreamSupplier
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

class PasswordTreeSecretHandlingTest {

    @field:TempDir
    lateinit var passwordTreeDirectory: Path

    private val configuration = mockk<Configuration>()
    private val systemOperation = spyk(SystemOperation())
    private val cryptoProvider = createAesGcmCipherForTesting()
    private val passwordTreeEnvelope = PasswordTreeEnvelope()

    @BeforeEach
    fun setup() {
        fakeConfiguration(instance = configuration, withPasswordTreeLocation = passwordTreeDirectory.toString())
    }

    @Test
    fun `should scramble decrypted password tree shell after restore`() {
        val decryptedShell = spyk(shellOf("decrypted payload"))
        val passwordTreePayloadReader = mockk<PasswordTreePayloadReader>()
        every { passwordTreePayloadReader.read(any()) } returns PasswordTreeSnapshot()
        val passwordTreeFile = passwordTreeDirectory.resolve(PASSWORD_TREE_FILENAME)
        Files.write(passwordTreeFile, passwordTreeEnvelope.wrap(cryptoProvider.encrypt(shellOf("payload")).toByteArray()))

        PasswordTreeReader(
            systemOperation = systemOperation,
            configuration = configuration,
            cryptoProvider = mockk {
                every { decrypt(any()) } returns decryptedShell
            },
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadReader = passwordTreePayloadReader,
            legacyCurrentPasswordTreePayloadReader = LegacyCurrentPasswordTreePayloadReader(configuration, systemOperation),
        ).restore()

        verify(exactly = 1) { decryptedShell.scramble() }
    }

    @Test
    fun `should scramble plaintext password tree shell after sync`() {
        val payloadShell = spyk(shellOf("plaintext payload"))
        val passwordTreePayloadWriter = mockk<PasswordTreePayloadWriter>()
        every { passwordTreePayloadWriter.write(any()) } returns payloadShell

        PasswordTreeWriter(
            systemOperation = systemOperation,
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadWriter = passwordTreePayloadWriter,
        ).sync(EggStreamSupplier({ Stream.empty() }))

        verify(exactly = 1) { payloadShell.scramble() }
    }
}
