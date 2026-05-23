package de.pflugradts.passbird.application.boot.setup

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
class PassbirdSetupModuleTest {
    @Test
    fun `should resolve all dependencies`() {
        // given / when
        val runContext = PassbirdRunContext("/tmp".toDirectory(), Slot.DEFAULT)
        val actual = Guice.createInjector(SetupModule(runContext))
            .getInstance(PassbirdTestSetup::class.java)

        // then
        expectThat(actual.bootable).isA<PassbirdSetup>()
        expectThat(actual.runContext) isSameInstanceAs runContext
    }

    private class PassbirdTestSetup @Inject constructor(
        @Inject val bootable: Bootable,
        @Inject val runContext: RunContext,
    )
}
