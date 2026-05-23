package de.pflugradts.passbird.application.boot.setup

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
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
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.emptyInput
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.fakeInput
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
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
        val password1 = fakeInput("p4s5w0rD")
        val password2 = fakeInput("p4s5w0rD")
        val pathSlot = slot<Path>()
        fakeConfiguration(
            instance = configuration,
            withConfigurationTemplate = true,
            withPasswordTreeLocation = configurationDirectory,
            withKeyStoreLocation = configurationDirectory,
        )
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(password1, password2),
            withReceiveConfirmation = true,
        )
        fakeSystemOperation(
            instance = systemOperation,
            withPaths = listOf(Pair(VALID_DIRECTORY, fakePath(exists = true, isDirectory = true))),
        )
        every { configurationSync.sync(configurationDirectory.toDirectory()) } returns success(Unit)
        every { keyStoreAdapterPort.storeKey(eq(password1.shell.toPlainShell()), capture(pathSlot)) } returns Unit

        // when
        passbirdSetup.boot()

        // then
        verify(exactly = 1) { setupGuide.sendWelcome() }
        verify(exactly = 1) { setupGuide.sendConfigTemplateRouteInformation() }
        verify(exactly = 1) { setupGuide.sendInputPath("configuration") }
        verify(exactly = 1) { setupGuide.sendCreateKeyStoreInformation() }
        verify(exactly = 0) { setupGuide.sendNonMatchingInputs() }
        expectThat(pathSlot.captured.fileName.name) isEqualTo ReadableConfiguration.KEYSTORE_FILENAME
        expectThat(pathSlot.captured.parent.name) isEqualTo configurationDirectory
        expectThat(password1.shell.asString()) isNotEqualTo "p4s5w0rD"
        expectThat(password2.shell.asString()) isNotEqualTo "p4s5w0rD"
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
    fun `should abort config template route after failed configuration sync`() {
        // given
        val configurationDirectory = VALID_DIRECTORY
        fakeConfiguration(
            instance = configuration,
            withConfigurationTemplate = true,
            withPasswordTreeLocation = configurationDirectory,
        )
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = true)
        fakeSystemOperation(
            instance = systemOperation,
            withPaths = listOf(Pair(VALID_DIRECTORY, fakePath(exists = true, isDirectory = true))),
        )
        every {
            configurationSync.sync(configurationDirectory.toDirectory())
        } returns failure(IllegalStateException("disk full"))

        // when
        passbirdSetup.boot()

        // then
        verify(exactly = 1) { setupGuide.sendWelcome() }
        verify(exactly = 1) { setupGuide.sendConfigTemplateRouteInformation() }
        verify(exactly = 1) { setupGuide.sendInputPath("configuration") }
        verify(exactly = 0) { setupGuide.sendCreateKeyStoreInformation() }
        verify(exactly = 0) { setupGuide.sendRestart() }
        verify(exactly = 1) { setupGuide.sendGoodbye() }
        verify(exactly = 1) { systemOperation.exit() }
        verify { keyStoreAdapterPort wasNot Called }
    }

    @Test
    fun `should run config key store route`() {
        // given
        val configurationDirectory = VALID_DIRECTORY
        val password1 = fakeInput("p4s5w0rD")
        val password2 = fakeInput("p4s5w0rD")
        val pathSlot = slot<Path>()
        fakeConfiguration(instance = configuration, withKeyStoreLocation = configurationDirectory)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(password1, password2),
            withReceiveConfirmation = true,
        )
        fakeSystemOperation(
            instance = systemOperation,
            withPaths = listOf(Pair(VALID_DIRECTORY, fakePath(exists = true, isDirectory = true))),
        )
        every { keyStoreAdapterPort.storeKey(eq(password1.shell.toPlainShell()), capture(pathSlot)) } returns Unit

        // when
        passbirdSetup.boot()

        // then
        verify(exactly = 1) { setupGuide.sendWelcome() }
        verify(exactly = 1) { setupGuide.sendConfigKeyStoreRouteInformation(configurationDirectory) }
        verify(exactly = 1) { setupGuide.sendInputPath("keystore") }
        verify(exactly = 1) { setupGuide.sendCreateKeyStoreInformation() }
        verify(exactly = 0) { setupGuide.sendNonMatchingInputs() }
        expectThat(pathSlot.captured.fileName.name) isEqualTo ReadableConfiguration.KEYSTORE_FILENAME
        expectThat(pathSlot.captured.parent.name) isEqualTo configurationDirectory
        expectThat(password1.shell.asString()) isNotEqualTo "p4s5w0rD"
        expectThat(password2.shell.asString()) isNotEqualTo "p4s5w0rD"
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
        val password1 = fakeInput("p4s5w0rD")
        val password2 = fakeInput("p4s5w0rD")
        val pathSlot = slot<Path>()
        fakeConfiguration(instance = configuration, withKeyStoreLocation = invalidConfigurationDirectory)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(fakeInput(nonexistentConfigurationDirectory), validDirectory),
            withTheseSecureInputs = listOf(password1, password2),
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
        every { keyStoreAdapterPort.storeKey(eq(password1.shell.toPlainShell()), capture(pathSlot)) } returns Unit

        // when
        passbirdSetup.boot()

        // then
        verify(exactly = 1) { setupGuide.sendWelcome() }
        expectThat(pathSlot.captured.fileName.name) isEqualTo ReadableConfiguration.KEYSTORE_FILENAME
        expectThat(pathSlot.captured.parent.name) isEqualTo VALID_DIRECTORY
        expectThat(password1.shell.asString()) isNotEqualTo "p4s5w0rD"
        expectThat(password2.shell.asString()) isNotEqualTo "p4s5w0rD"
        verify(exactly = 1) { setupGuide.sendGoodbye() }
        verify(exactly = 1) { systemOperation.exit() }
    }

    @Test
    fun `should create key store with matching password input`() {
        // given
        val configurationDirectory = VALID_DIRECTORY
        val passwordMismatch1 = fakeInput("bassword")
        val passwordMismatch2 = fakeInput("guessword")
        val emptyPassword1 = emptyInput()
        val emptyPassword2 = emptyInput()
        val passwordMatched1 = fakeInput("p4s5w0rD")
        val passwordMatched2 = fakeInput("p4s5w0rD")
        val pathSlot = slot<Path>()
        fakeConfiguration(instance = configuration, withKeyStoreLocation = configurationDirectory)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(
                passwordMismatch1,
                passwordMismatch2,
                emptyPassword1,
                emptyPassword2,
                passwordMatched1,
                passwordMatched2,
            ),
            withReceiveConfirmation = true,
        )
        fakeSystemOperation(
            instance = systemOperation,
            withPaths = listOf(Pair(VALID_DIRECTORY, fakePath(exists = true, isDirectory = true))),
        )
        every { keyStoreAdapterPort.storeKey(eq(passwordMatched1.shell.toPlainShell()), capture(pathSlot)) } returns Unit

        // when
        passbirdSetup.boot()

        // then
        verify(exactly = 1) { setupGuide.sendWelcome() }
        verify(exactly = 1) { setupGuide.sendConfigKeyStoreRouteInformation(configurationDirectory) }
        verify(exactly = 1) { setupGuide.sendInputPath("keystore") }
        verify(exactly = 1) { setupGuide.sendCreateKeyStoreInformation() }
        verify(exactly = 2) { setupGuide.sendNonMatchingInputs() }
        expectThat(pathSlot.captured.fileName.name) isEqualTo ReadableConfiguration.KEYSTORE_FILENAME
        expectThat(pathSlot.captured.parent.name) isEqualTo configurationDirectory
        expectThat(passwordMismatch1.shell.asString()) isNotEqualTo "bassword"
        expectThat(passwordMismatch2.shell.asString()) isNotEqualTo "guessword"
        expectThat(passwordMatched1.shell.asString()) isNotEqualTo "p4s5w0rD"
        expectThat(passwordMatched2.shell.asString()) isNotEqualTo "p4s5w0rD"
        verify(exactly = 1) { setupGuide.sendRestart() }
        verify(exactly = 1) { setupGuide.sendGoodbye() }
        verify(exactly = 1) { systemOperation.exit() }
        verify { configurationSync wasNot Called }
    }

    @Test
    fun `should only warn about non matching password input after the first failed attempt`() {
        // given
        val configurationDirectory = VALID_DIRECTORY
        val passwordMismatch1 = fakeInput("bassword")
        val passwordMismatch2 = fakeInput("guessword")
        val passwordMatched1 = fakeInput("p4s5w0rD")
        val passwordMatched2 = fakeInput("p4s5w0rD")
        val pathSlot = slot<Path>()
        fakeConfiguration(instance = configuration, withKeyStoreLocation = configurationDirectory)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(
                passwordMismatch1,
                passwordMismatch2,
                passwordMatched1,
                passwordMatched2,
            ),
            withReceiveConfirmation = true,
        )
        fakeSystemOperation(
            instance = systemOperation,
            withPaths = listOf(Pair(VALID_DIRECTORY, fakePath(exists = true, isDirectory = true))),
        )
        every { keyStoreAdapterPort.storeKey(eq(passwordMatched1.shell.toPlainShell()), capture(pathSlot)) } returns Unit

        // when
        passbirdSetup.boot()

        // then
        verifyOrder {
            userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("first input: ")))
            userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("second input: ")))
            userInterfaceAdapterPort.send(outputOf(shellOf("Your inputs do not match, please repeat.")))
            userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("first input: ")))
            userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("second input: ")))
        }
        expectThat(pathSlot.captured.fileName.name) isEqualTo ReadableConfiguration.KEYSTORE_FILENAME
        expectThat(pathSlot.captured.parent.name) isEqualTo configurationDirectory
        expectThat(passwordMismatch1.shell.asString()) isNotEqualTo "bassword"
        expectThat(passwordMismatch2.shell.asString()) isNotEqualTo "guessword"
        expectThat(passwordMatched1.shell.asString()) isNotEqualTo "p4s5w0rD"
        expectThat(passwordMatched2.shell.asString()) isNotEqualTo "p4s5w0rD"
    }
}
