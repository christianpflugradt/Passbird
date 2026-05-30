package de.pflugradts.passbird.application.boot.launcher

import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.KEYSTORE_FILENAME
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.process.migration.MigrationRequest
import de.pflugradts.passbird.application.process.migration.PendingMigration
import de.pflugradts.passbird.application.process.migration.PreLaunchMigrationLocator
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.unmockMain
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakePath
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.slot.Slot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly

class PassbirdLauncherTest {

    private val configuration = mockk<Configuration>(relaxed = true)
    private val preLaunchMigrationLocator = mockk<PreLaunchMigrationLocator>()
    private val runContext: RunContext = PassbirdRunContext("/tmp".toDirectory(), Slot.DEFAULT)
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val systemOperation = mockk<SystemOperation>(relaxed = true)
    private val bootedRoutes = mutableListOf<String>()
    private val passbirdLauncher = PassbirdLauncher(
        configuration,
        preLaunchMigrationLocator,
        runContext,
        userInterfaceAdapterPort,
        systemOperation,
        setupBoot = { bootedRoutes.add("setup") },
        migrationBoot = { _, _ -> bootedRoutes.add("migration") },
        applicationBoot = { bootedRoutes.add("application") },
    )

    @BeforeEach
    fun setup() {
        bootedRoutes.clear()
        every { preLaunchMigrationLocator.detect() } returns MigrationRequest.empty()
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
                fakePath(),
            ),
        )
        every { systemOperation.exists(any<java.nio.file.Path>()) } returns true

        // when
        passbirdLauncher.boot()

        // then
        expectThat(bootedRoutes).containsExactly("application")
    }

    @Test
    fun `should launch migration if migration is required`() {
        // given
        val keyStoreDirectoryName = "/tmp"
        val keyStoreFileName = KEYSTORE_FILENAME
        val keyStoreFilePath = fakePath()
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectoryName)
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                keyStoreDirectoryName.toDirectory(),
                keyStoreFileName.toFileName(),
                keyStoreFilePath,
            ),
        )
        every { preLaunchMigrationLocator.detect() } returns MigrationRequest(setOf(PendingMigration("keystore-format")))
        every { systemOperation.exists(keyStoreFilePath) } returns true

        // when
        passbirdLauncher.boot()

        // then
        expectThat(bootedRoutes).containsExactly("migration")
    }

    @Test
    fun `should launch setup if key store does not exist`() {
        // given
        val keyStoreDirectoryName = "/tmp"
        val keyStoreFileName = KEYSTORE_FILENAME
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectoryName)
        val keyStoreFilePath = fakePath()
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                keyStoreDirectoryName.toDirectory(),
                keyStoreFileName.toFileName(),
                keyStoreFilePath,
            ),
        )
        every { systemOperation.exists(keyStoreFilePath) } returns false

        // when
        passbirdLauncher.boot()

        // then
        expectThat(bootedRoutes).containsExactly("setup")
        verify(exactly = 0) { preLaunchMigrationLocator.detect() }
    }

    @Test
    fun `should launch setup if key store location is not set`() {
        // given
        val keyStoreDirectoryName = ""
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectoryName)

        // when
        passbirdLauncher.boot()

        // then
        expectThat(bootedRoutes).containsExactly("setup")
        verify(exactly = 0) { preLaunchMigrationLocator.detect() }
    }
}
