package de.pflugradts.passbird.application.boot.launcher

import com.google.inject.Guice
import com.google.inject.Inject
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.boot.Bootable
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA

@Tag(INTEGRATION)
class PassbirdLauncherModuleTest {
    @Test
    fun `should resolve all dependencies`() {
        // given / when
        val actual = Guice.createInjector(LauncherModule()).getInstance(PassbirdTestLauncher::class.java)

        // then
        actual as PassbirdTestLauncher
        expectThat(actual.bootable).isA<PassbirdLauncher>()
    }

    private class PassbirdTestLauncher @Inject constructor(@Inject val bootable: Bootable)
}
