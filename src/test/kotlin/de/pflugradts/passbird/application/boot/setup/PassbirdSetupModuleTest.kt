package de.pflugradts.passbird.application.boot.setup

import com.google.inject.Guice
import com.google.inject.Inject
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.boot.Bootable
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA

@Tag(INTEGRATION)
class PassbirdSetupModuleTest {
    @Test
    fun `should resolve all dependencies`() {
        // given / when
        val actual = Guice.createInjector(SetupModule())
            .getInstance(PassbirdTestSetup::class.java)

        // then
        expectThat(actual.bootable).isA<PassbirdSetup>()
    }

    private class PassbirdTestSetup @Inject constructor(@Inject val bootable: Bootable)
}
