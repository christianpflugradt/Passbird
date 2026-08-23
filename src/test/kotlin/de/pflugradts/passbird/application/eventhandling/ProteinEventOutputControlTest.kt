package de.pflugradts.passbird.application.eventhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.shell.fakeEnc
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(INTEGRATION)
class ProteinEventOutputControlTest {

    @Test
    fun `should suppress protein event output while active`() {
        val cryptoProvider = mockk<CryptoProvider>()
        val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
        val proteinEventOutputControl = ProteinEventOutputControl()
        val applicationEventHandler = ApplicationEventHandler(cryptoProvider, userInterfaceAdapterPort, proteinEventOutputControl)
        val eventRegistry = PassbirdEventRegistry(setOf(applicationEventHandler))
        val egg = createEggForTesting(
            withProteins = mapOf(Slot.DEFAULT to ShellPair(shellOf("old"), shellOf("secret"))),
        )
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        every { cryptoProvider.decrypt(any(EncryptedShell::class)) } answers { shellOf("decrypted") }
        eventRegistry.register(egg)
        eventRegistry.clearEvents()

        proteinEventOutputControl.suppress {
            egg.updateProtein(Slot.DEFAULT, shellOf("new").fakeEnc(), shellOf("updated").fakeEnc())
            eventRegistry.register(egg)
            eventRegistry.processEvents()
        }

        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
    }
}
