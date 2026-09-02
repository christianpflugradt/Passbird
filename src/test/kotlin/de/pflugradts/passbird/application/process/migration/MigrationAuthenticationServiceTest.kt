package de.pflugradts.passbird.application.process.migration

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.SecureInputUnavailableException
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.KEYSTORE_FILENAME
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakePath
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class MigrationAuthenticationServiceTest {

    private val configuration = mockk<Configuration>()
    private val keyStoreAdapterPort = mockk<KeyStoreAdapterPort>()
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>()
    private val systemOperation = mockk<SystemOperation>()
    private val migrationAuthenticationService = MigrationAuthenticationService(
        configuration = configuration,
        keyStoreAdapterPort = keyStoreAdapterPort,
        userInterfaceAdapterPort = userInterfaceAdapterPort,
        systemOperation = systemOperation,
    )

    @Test
    fun `should fail authentication when secure input is unavailable`() {
        // given
        every { userInterfaceAdapterPort.receiveSecurely(any()) } throws SecureInputUnavailableException()

        // when
        val actual = migrationAuthenticationService.authenticate(maxAttempts = 1)

        // then
        expectThat(actual.failure).isTrue()
        verify(exactly = 0) { keyStoreAdapterPort.loadKey(any(), any()) }
    }

    @Test
    fun `should cache successful migration credentials until invalidated`() {
        // given
        val keyStoreDirectory = "tmp"
        val keyStoreFilePath = fakePath()
        val firstInput = inputOf(shellOf("letmein1"))
        val secondInput = inputOf(shellOf("letmein2"))
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                keyStoreDirectory.toDirectory(),
                KEYSTORE_FILENAME.toFileName(),
                keyStoreFilePath,
            ),
        )
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectory)
        every { userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter key: "))) } returnsMany listOf(firstInput, secondInput)
        every { keyStoreAdapterPort.loadKey(any(), eq(keyStoreFilePath)) } returns success(shellOf("migration-key"))

        // when
        val firstAuthentication = migrationAuthenticationService.authenticate(maxAttempts = 1)
        val cachedAuthentication = migrationAuthenticationService.authenticate(maxAttempts = 1)
        migrationAuthenticationService.invalidate()
        val secondAuthentication = migrationAuthenticationService.authenticate(maxAttempts = 1)

        // then
        expectThat(firstAuthentication.failure).isFalse()
        expectThat(cachedAuthentication.failure).isFalse()
        expectThat(secondAuthentication.failure).isFalse()
        verify(exactly = 2) { userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter key: "))) }
        verify(exactly = 2) { keyStoreAdapterPort.loadKey(any(), eq(keyStoreFilePath)) }
    }

    @Test
    fun `should ignore invalidation when no migration credentials are cached`() {
        // when
        migrationAuthenticationService.invalidate()

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.receiveSecurely(any()) }
        verify(exactly = 0) { keyStoreAdapterPort.loadKey(any(), any()) }
    }

    @Test
    fun `should retry a failed keystore unlock and cache the successful credentials`() {
        // given
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
        every { userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter key: "))) } returnsMany listOf(
            inputOf(shellOf("incorrect")),
            inputOf(shellOf("correct")),
        )
        every { keyStoreAdapterPort.loadKey(any(), eq(keyStoreFilePath)) } returnsMany listOf(
            failure(IllegalArgumentException("incorrect password")),
            success(shellOf("migration-key")),
        )

        // when
        val authentication = migrationAuthenticationService.authenticate(maxAttempts = 2)
        val cachedAuthentication = migrationAuthenticationService.authenticate(maxAttempts = 2)

        // then
        expectThat(authentication.failure).isFalse()
        expectThat(cachedAuthentication.failure).isFalse()
        verify(exactly = 2) { userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter key: "))) }
        verify(exactly = 2) { keyStoreAdapterPort.loadKey(any(), eq(keyStoreFilePath)) }
    }

    @Test
    fun `should stop retrying after the configured number of failed keystore unlocks`() {
        // given
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
        every { userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter key: "))) } returnsMany listOf(
            inputOf(shellOf("incorrect-1")),
            inputOf(shellOf("incorrect-2")),
        )
        every { keyStoreAdapterPort.loadKey(any(), eq(keyStoreFilePath)) } returns failure(IllegalArgumentException("incorrect password"))

        // when
        val authentication = migrationAuthenticationService.authenticate(maxAttempts = 2)

        // then
        expectThat(authentication.failure).isTrue()
        verify(exactly = 2) { userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter key: "))) }
        verify(exactly = 2) { keyStoreAdapterPort.loadKey(any(), eq(keyStoreFilePath)) }
    }
}
