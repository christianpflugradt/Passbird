package de.pflugradts.passbird.application.boot.launcher

import com.google.inject.Guice
import com.google.inject.Inject
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.domain.model.slot.Slot
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
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
    }

    private class PassbirdTestLauncher @Inject constructor(
        @Inject val bootable: Bootable,
        @Inject val runContext: RunContext,
    )
}
