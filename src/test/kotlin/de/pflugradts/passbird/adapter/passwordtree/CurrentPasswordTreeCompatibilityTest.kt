package de.pflugradts.passbird.adapter.passwordtree

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.passwordtree.LegacyCurrentPasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.LegacyCurrentPasswordTreePayloadWriter
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreeSnapshot
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
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
class CurrentPasswordTreeCompatibilityTest {
    private val configuration = mockk<Configuration>()
    private val systemOperation = SystemOperation()
    private val cryptoProvider = createAesGcmCipherForTesting()
    private val passwordTreeEnvelope = PasswordTreeEnvelope()
    private val legacyCurrentPasswordTreePayloadWriter = LegacyCurrentPasswordTreePayloadWriter()
    private lateinit var passwordTreeDirectory: Path
    private lateinit var passwordTreeFile: Path

    @BeforeEach
    fun setup() {
        passwordTreeDirectory = Files.createTempDirectory("passbird-current-password-tree")
        passwordTreeFile = passwordTreeDirectory.resolve(ReadableConfiguration.PASSWORD_TREE_FILENAME)
        fakeConfiguration(instance = configuration, withPasswordTreeLocation = passwordTreeDirectory.toString())
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(passwordTreeDirectory.toString()).deleteRecursively()).isTrue()
    }

    @Test
    fun `should restore pre yolk current password tree format without migration`() {
        val egg = createEgg(
            slot = Slot.DEFAULT,
            eggIdShell = cryptoProvider.encrypt(shellOf("email")),
            passwordShell = cryptoProvider.encrypt(shellOf("Password1")),
        )
        Files.write(
            passwordTreeFile,
            passwordTreeEnvelope.wrapLegacyCurrent(
                cryptoProvider.encrypt(legacyCurrentPasswordTreePayloadWriter.write(PasswordTreeSnapshot(eggs = listOf(egg))))
                    .toByteArray(),
            ),
        )

        val restored = PasswordTreeReader(
            systemOperation = systemOperation,
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadReader = PasswordTreePayloadReader(configuration, systemOperation),
            legacyCurrentPasswordTreePayloadReader = LegacyCurrentPasswordTreePayloadReader(configuration, systemOperation),
        ).restore()

        val restoredEgg = restored.get().toList().single()
        expectThat(cryptoProvider.decrypt(restoredEgg.viewEggId()).asString()) isEqualTo "email"
        expectThat(cryptoProvider.decrypt(restoredEgg.viewPassword()).asString()) isEqualTo "Password1"
        expectThat(restoredEgg.hasYolk()).isFalse()
    }
}
