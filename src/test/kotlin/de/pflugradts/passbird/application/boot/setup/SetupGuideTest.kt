package de.pflugradts.passbird.application.boot.setup

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class SetupGuideTest {
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val setupGuide = SetupGuide(userInterfaceAdapterPort)

    @Test
    fun `should explain how to continue when keystore is missing`() {
        // when
        setupGuide.sendConfigKeyStoreRouteInformation("/tmp")

        // then
        verify(exactly = 1) {
            userInterfaceAdapterPort.send(
                outputOf(shellOf("To (c)ontinue setup, press 'c'. To quit setup, press any other key")),
            )
        }
    }
}
