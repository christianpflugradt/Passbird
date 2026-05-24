package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.keystore.KeyStoreService
import de.pflugradts.passbird.application.commandhandling.handler.ChangeMasterPasswordCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.application.security.KeyStoreAuthenticationService
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contentEquals
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Tag(INTEGRATION)
class ChangeMasterPasswordCommandIntegrationTest {

    private val userInterfaceAdapterPort = mockk<de.pflugradts.passbird.application.UserInterfaceAdapterPort>()
    private val systemOperation = SystemOperation()
    private val keyStoreService = KeyStoreService(systemOperation)
    private lateinit var tempDirectory: String
    private lateinit var keyStoreFile: Path
    private lateinit var passwordTreeFile: Path

    @BeforeEach
    fun setup() {
        tempDirectory = UUID.randomUUID().toString()
        expectThat(File(tempDirectory).mkdir()).isTrue()
        keyStoreFile = Path.of(tempDirectory, ReadableConfiguration.KEYSTORE_FILENAME)
        passwordTreeFile = Path.of(tempDirectory, ReadableConfiguration.PASSWORD_TREE_FILENAME)
    }

    @AfterEach
    fun cleanup() {
        Files.deleteIfExists(passwordTreeFile)
        Files.deleteIfExists(keyStoreFile)
        expectThat(File(tempDirectory).delete()).isTrue()
    }

    @Test
    fun `should rotate keystore password without touching password tree`() {
        // given
        val oldPassword = "p4s5wrD"
        val newPassword = "n3wp4s5"
        val originalTreeBytes = byteArrayOf(1, 2, 3, 4, 5)
        val configuration = Configuration().apply {
            adapter.keyStore.location = tempDirectory
            adapter.passwordTree.location = tempDirectory
        }
        val authenticationService = KeyStoreAuthenticationService(
            configuration = configuration,
            keyStoreAdapterPort = keyStoreService,
            userInterfaceAdapterPort = userInterfaceAdapterPort,
            systemOperation = systemOperation,
        )
        val inputHandler = createInputHandlerFor(
            ChangeMasterPasswordCommandHandler(
                keyStoreAdapterPort = keyStoreService,
                keyStoreAuthenticationService = authenticationService,
                userInterfaceAdapterPort = userInterfaceAdapterPort,
            ),
        )
        val originalSecret = plainShellOf(oldPassword.toCharArray()).let { password ->
            keyStoreService.storeKey(password, keyStoreFile)
            keyStoreService.loadKey(plainShellOf(oldPassword.toCharArray()), keyStoreFile).getOrNull()!!
        }
        Files.write(passwordTreeFile, originalTreeBytes)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(
                inputOf(shellOf(oldPassword)),
                inputOf(shellOf(newPassword)),
                inputOf(shellOf(newPassword)),
            ),
        )

        // when
        inputHandler.handleInput(inputOf(shellOf("k")))
        val oldPasswordLoad = keyStoreService.loadKey(plainShellOf(oldPassword.toCharArray()), keyStoreFile)
        val newPasswordLoad = keyStoreService.loadKey(plainShellOf(newPassword.toCharArray()), keyStoreFile)

        // then
        expectThat(oldPasswordLoad.success).isFalse()
        expectThat(newPasswordLoad.success).isTrue()
        expectThat(newPasswordLoad.getOrNull()) isEqualTo originalSecret
        expectThat(Files.readAllBytes(passwordTreeFile)).contentEquals(originalTreeBytes)
    }
}
