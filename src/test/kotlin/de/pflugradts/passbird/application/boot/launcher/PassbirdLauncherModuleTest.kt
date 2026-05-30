package de.pflugradts.passbird.application.boot.launcher

import com.google.inject.Guice
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.boot.expectedMultibinderClasses
import de.pflugradts.passbird.application.boot.implementationClasses
import de.pflugradts.passbird.application.process.migration.PreLaunchMigrationDetector
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.domain.model.slot.Slot
import jakarta.inject.Inject
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

@Tag(INTEGRATION)
class PassbirdLauncherModuleTest {
    @Test
    fun `should resolve all dependencies`() {
        // given / when
        val runContext = PassbirdRunContext("/tmp".toDirectory(), Slot.DEFAULT)
        val actual = Guice.createInjector(LauncherModule(runContext)).getInstance(PassbirdTestLauncher::class.java)

        // then
        actual as PassbirdTestLauncher
        expectThat(actual.bootable).isA<PassbirdLauncher>()
        expectThat(actual.runContext) isSameInstanceAs runContext
        expectThat(actual.preLaunchMigrationDetectors.implementationClasses()) isEqualTo expectedMultibinderClasses(
            PreLaunchMigrationDetector::class.java,
        )
    }

    private class PassbirdTestLauncher @Inject constructor(
        val bootable: Bootable,
        val runContext: RunContext,
        val preLaunchMigrationDetectors: Set<PreLaunchMigrationDetector>,
    )
}
