package de.pflugradts.passbird.application.eventhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggMoved
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.event.EggsExported
import de.pflugradts.passbird.domain.model.event.EggsImported
import de.pflugradts.passbird.domain.model.event.NestCreated
import de.pflugradts.passbird.domain.model.event.NestDiscarded
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S1
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
    @MethodSource("provideEggEvents")
    fun `should process egg events`(domainEvent: DomainEvent) {
        // given
        val expectedEggIdShell = shellOf("expected eggId")
        every { cryptoProvider.decrypt(any(EncryptedShell::class)) } answers { expectedEggIdShell }
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        val outputSlot = slot<Output>()

        // when
        passbirdEventRegistry.register(domainEvent)
        passbirdEventRegistry.processEvents()

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains expectedEggIdShell.asString()
    }

    @ParameterizedTest
    @MethodSource("provideImportExportEvents")
    fun `should process import export events`(domainEvent: DomainEvent) {
        // given
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        val outputSlot = slot<Output>()

        // when
        passbirdEventRegistry.register(domainEvent)
        passbirdEventRegistry.processEvents()

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains NUMBER_OF_EGGS.toString()
    }

    @ParameterizedTest
    @MethodSource("provideNestEvents")
    fun `should process nest events`(domainEvent: DomainEvent) {
        // given
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        val outputSlot = slot<Output>()

        // when
        passbirdEventRegistry.register(domainEvent)
        passbirdEventRegistry.processEvents()

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains NEST_NAME
    }

    companion object {

        @JvmStatic
        private fun provideEggEvents() = Stream.of(
            Arguments.of(EggCreated(createEggForTesting())),
            Arguments.of(EggDiscarded(createEggForTesting())),
            Arguments.of(EggUpdated(createEggForTesting())),
            Arguments.of(EggRenamed(createEggForTesting())),
            Arguments.of(EggMoved(createEggForTesting(withSlot = DEFAULT))),
            Arguments.of(EggMoved(createEggForTesting(withSlot = S1))),
            Arguments.of(EggNotFound(shellOf("expected eggId"))),
        )

        @JvmStatic
        private fun provideImportExportEvents() = Stream.of(
            Arguments.of(EggsExported(NUMBER_OF_EGGS)),
            Arguments.of(EggsImported(NUMBER_OF_EGGS)),
        )

        @JvmStatic
        private fun provideNestEvents() = Stream.of(
            Arguments.of(NestCreated(createNest(shellOf(NEST_NAME), S1))),
            Arguments.of(NestDiscarded(createNest(shellOf(NEST_NAME), S1))),
        )
    }
}

private const val NUMBER_OF_EGGS = 135
private const val NEST_NAME = "my nest"
