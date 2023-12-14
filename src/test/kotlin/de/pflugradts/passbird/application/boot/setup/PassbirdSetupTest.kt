package de.pflugradts.passbird.application.boot.setup

import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ConfigurationSync
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakePath
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.emptyInput
import de.pflugradts.passbird.domain.model.transfer.fakeInput
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path
import kotlin.io.path.name

private const val VALID_DIRECTORY = "tmp"

class PassbirdSetupTest {
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>()
    private val setupGuide = spyk(SetupGuide(userInterfaceAdapterPort))
    private val configurationSync = mockk<ConfigurationSync>()
    private val configuration = mockk<Configuration>(relaxed = true)
    private val keyStoreAdapterPort = mockk<KeyStoreAdapterPort>()
    private val systemOperation = mockk<SystemOperation>()
    private val passbirdSetup = PassbirdSetup(
        setupGuide = setupGuide,
        configurationSync = configurationSync,
        configuration = configuration,
        keyStoreAdapterPort = keyStoreAdapterPort,
        userInterfaceAdapterPort = userInterfaceAdapterPort,
        systemOperation = systemOperation,
    )

    @Test
    fun `should run config template route`() {
        // given
        val configurationDirectory = VALID_DIRECTORY
        val password = fakeInput("p4s5w0rD")
        val pathSlot = slot<Path>()
        fakeConfiguration(
            instance = configuration,
            withConfigurationTemplate = true,
            withPasswordStoreLocation = configurationDirectory,
            withKeyStoreLocation = configurationDirectory,
        )
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(password, password),
            withReceiveConfirmation = true,
        )
        fakeSystemOperation(
            instance = systemOperation,
            withPaths = listOf(Pair(VALID_DIRECTORY, fakePath(exists = true, isDirectory = true))),
        )
        every { configurationSync.sync(configurationDirectory.toDirectory()) } returns Unit
        every { keyStoreAdapterPort.storeKey(eq(password.shell.toPlainShell()), capture(pathSlot)) } returns Unit

        // when
        passbirdSetup.boot()

        // then
        verify(exactly = 1) { setupGuide.sendWelcome() }
        verify(exactly = 1) { setupGuide.sendConfigTemplateRouteInformation() }
        verify(exactly = 1) { setupGuide.sendInputPath("configuration") }
        verify(exactly = 1) { setupGuide.sendCreateKeyStoreInformation() }
        expectThat(pathSlot.captured.fileName.name) isEqualTo ReadableConfiguration.KEYSTORE_FILENAME
        expectThat(pathSlot.captured.parent.name) isEqualTo configurationDirectory
        verify(exactly = 1) { setupGuide.sendRestart() }
        verify(exactly = 1) { setupGuide.sendGoodbye() }
        verify(exactly = 1) { systemOperation.exit() }
    }

    @Test
    fun `should abort unconfirmed config template route`() {
        // given
        fakeConfiguration(instance = configuration, withConfigurationTemplate = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)
        fakeSystemOperation(instance = systemOperation)

        // when
        passbirdSetup.boot()

        // then
        verify(exactly = 1) { setupGuide.sendWelcome() }
        verify(exactly = 1) { setupGuide.sendConfigTemplateRouteInformation() }
        verify(exactly = 1) { setupGuide.sendGoodbye() }
        verify(exactly = 1) { systemOperation.exit() }
        verify { configurationSync wasNot Called }
        verify { keyStoreAdapterPort wasNot Called }
    }

    @Test
    fun `should run config key store route`() {
        // given
        val configurationDirectory = VALID_DIRECTORY
        val password = fakeInput("p4s5w0rD")
        val pathSlot = slot<Path>()
        fakeConfiguration(instance = configuration, withKeyStoreLocation = configurationDirectory)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(password, password),
            withReceiveConfirmation = true,
        )
        fakeSystemOperation(
            instance = systemOperation,
            withPaths = listOf(Pair(VALID_DIRECTORY, fakePath(exists = true, isDirectory = true))),
        )
        every { keyStoreAdapterPort.storeKey(eq(password.shell.toPlainShell()), capture(pathSlot)) } returns Unit

        // when
        passbirdSetup.boot()

        // then
        verify(exactly = 1) { setupGuide.sendWelcome() }
        verify(exactly = 1) { setupGuide.sendConfigKeyStoreRouteInformation(configurationDirectory) }
        verify(exactly = 1) { setupGuide.sendInputPath("keystore") }
        verify(exactly = 1) { setupGuide.sendCreateKeyStoreInformation() }
        expectThat(pathSlot.captured.fileName.name) isEqualTo ReadableConfiguration.KEYSTORE_FILENAME
        expectThat(pathSlot.captured.parent.name) isEqualTo configurationDirectory
        verify(exactly = 1) { setupGuide.sendRestart() }
        verify(exactly = 1) { setupGuide.sendGoodbye() }
        verify(exactly = 1) { systemOperation.exit() }
        verify { configurationSync wasNot Called }
    }

    @Test
    fun `should abort unconfirmed config key store route`() {
        // given
        val configurationDirectory = VALID_DIRECTORY
        fakeConfiguration(instance = configuration, withKeyStoreLocation = configurationDirectory)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)
        fakeSystemOperation(instance = systemOperation)

        // when
        passbirdSetup.boot()

        // then
        verify(exactly = 1) { setupGuide.sendWelcome() }
        verify(exactly = 1) { setupGuide.sendConfigKeyStoreRouteInformation(configurationDirectory) }
        verify(exactly = 1) { setupGuide.sendGoodbye() }
        verify(exactly = 1) { systemOperation.exit() }
        verify { configurationSync wasNot Called }
        verify { keyStoreAdapterPort wasNot Called }
    }

    @Test
    fun `should accept corrected directory`() {
        // given
        val invalidConfigurationDirectory = "/dev/null"
        val nonexistentConfigurationDirectory = "/dev/none"
        val validDirectory = fakeInput(VALID_DIRECTORY)
        val password = fakeInput("p4s5w0rD")
        val pathSlot = slot<Path>()
        fakeConfiguration(instance = configuration, withKeyStoreLocation = invalidConfigurationDirectory)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(fakeInput(nonexistentConfigurationDirectory), validDirectory),
            withTheseSecureInputs = listOf(password, password),
            withReceiveConfirmation = true,
        )
        fakeSystemOperation(
            instance = systemOperation,
            withPaths = listOf(
                Pair(VALID_DIRECTORY, fakePath(exists = true, isDirectory = true)),
                Pair(invalidConfigurationDirectory, fakePath(exists = true, isDirectory = false)),
                Pair(nonexistentConfigurationDirectory, fakePath(exists = false, isDirectory = true)),
            ),
        )
        every { keyStoreAdapterPort.storeKey(eq(password.shell.toPlainShell()), capture(pathSlot)) } returns Unit

        // when
        passbirdSetup.boot()

        // then
        verify(exactly = 1) { setupGuide.sendWelcome() }
        expectThat(pathSlot.captured.fileName.name) isEqualTo ReadableConfiguration.KEYSTORE_FILENAME
        expectThat(pathSlot.captured.parent.name) isEqualTo VALID_DIRECTORY
        verify(exactly = 1) { setupGuide.sendGoodbye() }
        verify(exactly = 1) { systemOperation.exit() }
    }

    @Test
    fun `should create key store with matching password input`() {
        // given
        val configurationDirectory = VALID_DIRECTORY
        val passwordMismatch1 = fakeInput("bassword")
        val passwordMismatch2 = fakeInput("guessword")
        val emptyPassword = emptyInput()
        val passwordMatched = fakeInput("p4s5w0rD")
        val pathSlot = slot<Path>()
        fakeConfiguration(instance = configuration, withKeyStoreLocation = configurationDirectory)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(
                passwordMismatch1,
                passwordMismatch2,
                emptyPassword,
                emptyPassword,
                passwordMatched,
                passwordMatched,
            ),
            withReceiveConfirmation = true,
        )
        fakeSystemOperation(
            instance = systemOperation,
            withPaths = listOf(Pair(VALID_DIRECTORY, fakePath(exists = true, isDirectory = true))),
        )
        every { keyStoreAdapterPort.storeKey(eq(passwordMatched.shell.toPlainShell()), capture(pathSlot)) } returns Unit

        // when
        passbirdSetup.boot()

        // then
        verify(exactly = 1) { setupGuide.sendWelcome() }
        verify(exactly = 1) { setupGuide.sendConfigKeyStoreRouteInformation(configurationDirectory) }
        verify(exactly = 1) { setupGuide.sendInputPath("keystore") }
        verify(exactly = 1) { setupGuide.sendCreateKeyStoreInformation() }
        expectThat(pathSlot.captured.fileName.name) isEqualTo ReadableConfiguration.KEYSTORE_FILENAME
        expectThat(pathSlot.captured.parent.name) isEqualTo configurationDirectory
        verify(exactly = 1) { setupGuide.sendRestart() }
        verify(exactly = 1) { setupGuide.sendGoodbye() }
        verify(exactly = 1) { systemOperation.exit() }
        verify { configurationSync wasNot Called }
    }
}
