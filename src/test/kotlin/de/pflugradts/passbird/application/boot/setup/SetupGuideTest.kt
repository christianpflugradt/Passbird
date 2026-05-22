package de.pflugradts.passbird.application.boot.setup

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
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

    @Test
    fun `should explain how to continue when configuration is missing`() {
        // when
        setupGuide.sendConfigTemplateRouteInformation()

        // then
        verify(exactly = 1) {
            userInterfaceAdapterPort.send(
                outputOf(shellOf("To (c)ontinue setup, press 'c'. To quit setup, press any other key")),
            )
        }
    }

    @Test
    fun `should visually separate keystore warning from continue instructions`() {
        // when
        setupGuide.sendConfigKeyStoreRouteInformation("/tmp")

        // then
        verifyOrder {
            userInterfaceAdapterPort.send(outputOf(shellOf("However in that directory there is no file 'passbird.sec'")))
            userInterfaceAdapterPort.sendLineBreak()
            userInterfaceAdapterPort.send(
                outputOf(shellOf("To (c)ontinue setup, press 'c'. To quit setup, press any other key")),
            )
        }
    }
}
