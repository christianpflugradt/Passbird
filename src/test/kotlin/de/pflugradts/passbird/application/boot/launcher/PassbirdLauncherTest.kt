package de.pflugradts.passbird.application.boot.launcher

import com.google.inject.Module
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.main.ApplicationModule
import de.pflugradts.passbird.application.boot.setup.SetupModule
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.KEYSTORE_FILENAME
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.mockMain
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.unmockMain
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakePath
import de.pflugradts.passbird.application.util.fakeSystemOperation
import io.mockk.mockk
import io.mockk.slot
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
        mockMain(moduleSlot)
    }

    @AfterEach
    fun cleanup() {
        unmockMain()
    }

    @Test
    fun `should launch main application if key store exists`() {
        // given
        val keyStoreDirectoryName = "/tmp"
        val keyStoreFileName = KEYSTORE_FILENAME
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectoryName, withAnsiEscapeCodesEnabled = true)
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                keyStoreDirectoryName.toDirectory(),
                keyStoreFileName.toFileName(),
                fakePath(exists = true),
            ),
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
}
