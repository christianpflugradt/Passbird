package de.pflugradts.passbird.application.process.migration.passwordtree

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.passwordtree.PasswordTreeReader
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.passwordtree.PasswordTreeSnapshot
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.application.security.createTestKeyShell
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@Tag(INTEGRATION)
class PasswordTreeTrashMigrationServiceTest {
    private val configuration = mockk<Configuration>()
    private val systemOperation = SystemOperation()
    private val passwordTreeEnvelope = PasswordTreeEnvelope()
    private val passwordTreePayloadWriter = PasswordTreePayloadWriter()
    private lateinit var passwordTreeDirectory: Path
    private lateinit var passwordTreeFile: Path

    @BeforeEach
    fun setup() {
        passwordTreeDirectory = Files.createTempDirectory("passbird-trash-migration")
        passwordTreeFile = passwordTreeDirectory.resolve(ReadableConfiguration.PASSWORD_TREE_FILENAME)
        fakeConfiguration(
            instance = configuration,
            withPasswordTreeLocation = passwordTreeDirectory.toString(),
        )
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(passwordTreeDirectory.toString()).deleteRecursively()).isTrue()
    }

    @Test
    fun `should migrate legacy trash password tree content to current envelope without losing trash data`() {
        val cryptoProvider = createAesGcmCipherForTesting()
        val detector = PasswordTreeTrashMigrationDetector(configuration, systemOperation)
        val snapshot = PasswordTreeSnapshot(
            eggs = listOf(
                createEgg(
                    slot = S2,
                    eggIdShell = cryptoProvider.encrypt(shellOf("bank")),
                    passwordShell = cryptoProvider.encrypt(shellOf("Password2")),
                    trashed = true,
                    deletionEpochDay = 123,
                ),
            ),
        )
        Files.write(
            passwordTreeFile,
            wrapLegacyTrashPasswordTree(
                cryptoProvider.encrypt(passwordTreePayloadWriter.write(snapshot)).toByteArray(),
            ),
        )

        expectThat(detector.detect().required).isTrue()

        PasswordTreeTrashMigrationService(
            configuration = configuration,
            passwordTreePayloadReader = PasswordTreePayloadReader(configuration, systemOperation),
            passwordTreePayloadWriter = passwordTreePayloadWriter,
            systemOperation = systemOperation,
        ).migrate(createTestKeyShell())

        val restored = PasswordTreeReader(
            systemOperation = systemOperation,
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadReader = PasswordTreePayloadReader(configuration, systemOperation),
        ).restore()

        expectThat(detector.detect().required).isFalse()
        expectThat(passwordTreeEnvelope.isCurrent(Files.readAllBytes(passwordTreeFile))).isTrue()
        expectThat(restored.get().toList().single().isTrashed()).isTrue()
        expectThat(restored.get().toList().single().deletionEpochDay()) isEqualTo 123
    }
}
