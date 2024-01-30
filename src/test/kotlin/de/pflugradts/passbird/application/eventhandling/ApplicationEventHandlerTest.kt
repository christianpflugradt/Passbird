package de.pflugradts.passbird.application.eventhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggMoved
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import strikt.api.expectThat
import strikt.assertions.contains
import java.util.stream.Stream

@Tag(INTEGRATION)
class ApplicationEventHandlerTest {

    private val cryptoProvider = mockk<CryptoProvider>()
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>()
    private val applicationEventHandler = ApplicationEventHandler(cryptoProvider, userInterfaceAdapterPort)
    private var passbirdEventRegistry = PassbirdEventRegistry(mutableSetOf<EventHandler>(applicationEventHandler))

    @ParameterizedTest
    @MethodSource("providePasswordEvents")
    fun `should process egg created`(domainEvent: DomainEvent) {
        // given
        val expectedEggIdShell = shellOf("expected eggId")
        every { cryptoProvider.decrypt(any(Shell::class)) } answers { expectedEggIdShell }
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        val outputSlot = slot<Output>()

        // when
        passbirdEventRegistry.register(domainEvent)
        passbirdEventRegistry.processEvents()

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains expectedEggIdShell.asString()
    }

    companion object {
        @JvmStatic
        private fun providePasswordEvents() = Stream.of(
            Arguments.of(EggCreated(createEggForTesting())),
            Arguments.of(EggUpdated(createEggForTesting())),
            Arguments.of(EggRenamed(createEggForTesting())),
            Arguments.of(EggMoved(createEggForTesting())),
            Arguments.of(EggNotFound(emptyShell())),
        )
    }
}
