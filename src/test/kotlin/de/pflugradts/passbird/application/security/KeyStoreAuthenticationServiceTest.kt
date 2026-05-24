package de.pflugradts.passbird.application.security

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.KEYSTORE_FILENAME
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakePath
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.emptyInput
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import java.nio.file.Path

class KeyStoreAuthenticationServiceTest {

    private val configuration = mockk<Configuration>()
    private val keyStoreAdapterPort = mockk<KeyStoreAdapterPort>()
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>()
    private val systemOperation = mockk<SystemOperation>()
    private val keyStoreAuthenticationService = KeyStoreAuthenticationService(
        configuration = configuration,
        keyStoreAdapterPort = keyStoreAdapterPort,
        userInterfaceAdapterPort = userInterfaceAdapterPort,
        systemOperation = systemOperation,
    )

    @Test
    fun `should treat empty current password input as unsuccessful attempt`() {
        // given
        val emptyPassword = emptyInput()
        val correctPassword = inputOf(shellOf("letmein"))
        val keyStoreDirectory = "tmp"
        val keyStoreFilePath = fakePath()
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                keyStoreDirectory.toDirectory(),
                KEYSTORE_FILENAME.toFileName(),
                keyStoreFilePath,
            ),
        )
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectory)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(emptyPassword, correctPassword),
        )
        every {
            keyStoreAdapterPort.loadKey(match { it.toCharArray().isEmpty() }, eq(keyStoreFilePath))
        } returns failure(RuntimeException())
        every {
            keyStoreAdapterPort.loadKey(match { it.toCharArray().contentEquals("letmein".toCharArray()) }, eq(keyStoreFilePath))
        } returns success(emptyShell())

        // when
        val actual = keyStoreAuthenticationService.authenticate()

        // then
        expectThat(actual.success).isTrue()
        verify(exactly = 2) { keyStoreAdapterPort.loadKey(any(), eq(keyStoreFilePath)) }
        expectThat(correctPassword.shell.asString()) isNotEqualTo "letmein"
    }

    @Test
    fun `should fail authentication after 3 unsuccessful attempts`() {
        // given
        val incorrectPassword1 = inputOf(shellOf("letmeout1"))
        val incorrectPassword2 = inputOf(shellOf("letmeout2"))
        val incorrectPassword3 = inputOf(shellOf("letmeout3"))
        val keyStoreDirectory = "tmp"
        val keyStoreFilePath = fakePath()
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                keyStoreDirectory.toDirectory(),
                KEYSTORE_FILENAME.toFileName(),
                keyStoreFilePath,
            ),
        )
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectory)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(incorrectPassword1, incorrectPassword2, incorrectPassword3),
        )
        every { keyStoreAdapterPort.loadKey(any(), eq(keyStoreFilePath)) } returns failure(RuntimeException())

        // when
        val actual = keyStoreAuthenticationService.authenticate()

        // then
        expectThat(actual.failure).isTrue()
        verify(exactly = 3) { keyStoreAdapterPort.loadKey(any(), eq(keyStoreFilePath)) }
        expectThat(incorrectPassword1.shell.asString()) isNotEqualTo "letmeout1"
        expectThat(incorrectPassword2.shell.asString()) isNotEqualTo "letmeout2"
        expectThat(incorrectPassword3.shell.asString()) isNotEqualTo "letmeout3"
    }

    @Test
    fun `should use provided prompt during authentication`() {
        // given
        val incorrectPassword = inputOf(shellOf("letmeout"))
        val keyStoreDirectory = "tmp"
        val keyStoreFilePath = fakePath()
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                keyStoreDirectory.toDirectory(),
                KEYSTORE_FILENAME.toFileName(),
                keyStoreFilePath,
            ),
        )
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectory)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(incorrectPassword),
        )
        every { keyStoreAdapterPort.loadKey(any(), eq(keyStoreFilePath)) } returns failure(RuntimeException())

        // when
        keyStoreAuthenticationService.authenticate(maxAttempts = 1, prompt = "Enter current key: ")

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter current key: "))) }
    }

    @Test
    fun `should resolve configured key store path`() {
        // given
        val keyStoreDirectory = "tmp"
        val keyStoreFilePath = mockk<Path>()
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                keyStoreDirectory.toDirectory(),
                KEYSTORE_FILENAME.toFileName(),
                keyStoreFilePath,
            ),
        )
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectory)

        // when
        val actual = keyStoreAuthenticationService.keyStorePath()

        // then
        expectThat(actual).isNotEqualTo(null)
        verify(exactly = 1) {
            systemOperation.resolvePath(keyStoreDirectory.toDirectory(), KEYSTORE_FILENAME.toFileName())
        }
    }
}
