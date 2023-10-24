package de.pflugradts.passbird.application.boot.launcher

import com.google.inject.Module
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.main.ApplicationModule
import de.pflugradts.passbird.application.boot.setup.SetupModule
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.KEYSTORE_FILENAME
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakePath
import de.pflugradts.passbird.application.util.fakeSystemOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA

class PassbirdLauncherTest {

    private val configuration = mockk<Configuration>(relaxed = true)
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val systemOperation = mockk<SystemOperation>(relaxed = true)
    private val passbirdLauncher = PassbirdLauncher(configuration, userInterfaceAdapterPort, systemOperation)
    private val moduleSlot = slot<Module>()

    @BeforeEach
    fun setup() {
        System.clearProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY)
        mockkStatic(::bootModule)
        every { bootModule(capture(moduleSlot)) } returns Unit
    }

    @AfterEach
    fun cleanup() { unmockkAll() }

    @Test
    fun `should launch main application if key store exists`() {
        // given
        val keyStoreDirectoryName = "/tmp"
        val keyStoreFileName = KEYSTORE_FILENAME
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectoryName)
        val keyStoreFilePath = fakePath(exists = true)
        val keyStoreDirPath = fakePath(resolvingTo = Pair(keyStoreFilePath, keyStoreFileName))
        fakeSystemOperation(
            instance = systemOperation,
            withPaths = listOf(Pair(keyStoreDirectoryName, keyStoreDirPath)),
        )

        // when
        passbirdLauncher.boot()

        // then
        expectThat(moduleSlot.captured).isA<ApplicationModule>()
        expectThat(moduleSlot.captured).not().isA<SetupModule>()
    }

    @Test
    fun `should launch setup if key store does not exist`() {
        // given
        val keyStoreDirectoryName = "/tmp"
        val keyStoreFileName = KEYSTORE_FILENAME
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectoryName)
        val keyStoreFilePath = fakePath(exists = false)
        val keyStoreDirPath = fakePath(resolvingTo = Pair(keyStoreFilePath, keyStoreFileName))
        fakeSystemOperation(
            instance = systemOperation,
            withPaths = listOf(Pair(keyStoreDirectoryName, keyStoreDirPath)),
        )

        // when
        passbirdLauncher.boot()

        // then
        expectThat(moduleSlot.captured).isA<SetupModule>()
        expectThat(moduleSlot.captured).not().isA<ApplicationModule>()
    }

    @Test
    fun `should launch setup if key store location is not set`() {
        // given
        val keyStoreDirectoryName = ""
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectoryName)

        // when
        passbirdLauncher.boot()

        // then
        expectThat(moduleSlot.captured).isA<SetupModule>()
        expectThat(moduleSlot.captured).not().isA<ApplicationModule>()
    }

    @Test
    fun `should terminate application`() {
        // given / when
        passbirdLauncher.terminate(systemOperation)

        // then
        verify(exactly = 1) { systemOperation.exit() }
    }
}
