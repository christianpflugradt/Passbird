package de.pflugradts.passbird.application.boot.launcher

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.boot.expectedGraphClasses
import de.pflugradts.passbird.application.boot.implementationClasses
import de.pflugradts.passbird.application.process.migration.PreLaunchMigrationDetector
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.domain.model.slot.Slot
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

@Tag(INTEGRATION)
class PassbirdLauncherGraphTest {
    @Test
    fun `should resolve all dependencies`() {
        // given / when
        val runContext = PassbirdRunContext("/tmp".toDirectory(), Slot.DEFAULT)
        val actual = LauncherGraph(runContext)

        // then
        expectThat(actual.bootable).isA<PassbirdLauncher>()
        expectThat(actual.runContext) isSameInstanceAs runContext
        expectThat(actual.preLaunchMigrationDetectors.implementationClasses()) isEqualTo expectedGraphClasses(
            PreLaunchMigrationDetector::class.java,
        )
    }
}
