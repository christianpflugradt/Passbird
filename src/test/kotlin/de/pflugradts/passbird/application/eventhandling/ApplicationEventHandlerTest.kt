package de.pflugradts.passbird.application.eventhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.application.security.fakeCryptoProvider
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
import de.pflugradts.passbird.domain.model.event.ProteinCreated
import de.pflugradts.passbird.domain.model.event.ProteinDiscarded
import de.pflugradts.passbird.domain.model.event.ProteinUpdated
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.shell.fakeEnc
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
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
        val expectedEggId = "expected eggId"
        every { cryptoProvider.decrypt(any(EncryptedShell::class)) } answers { shellOf(expectedEggId) }
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        val outputSlot = slot<Output>()

        // when
        passbirdEventRegistry.register(domainEvent)
        passbirdEventRegistry.processEvents()

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains expectedEggId
    }

    @Test
    fun `should scramble decrypted shell after event output is prepared`() {
        // given
        val decryptedEggIdShell = spyk(shellOf("expected eggId"))
        every { cryptoProvider.decrypt(any(EncryptedShell::class)) } returns decryptedEggIdShell
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)

        // when
        passbirdEventRegistry.register(EggCreated(createEggForTesting()))
        passbirdEventRegistry.processEvents()

        // then
        verify(exactly = 1) { decryptedEggIdShell.scramble() }
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

    @Test
    fun `should process protein created event`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        egg.clearDomainEvents()
        egg.updateProtein(S1, shellOf("user").fakeEnc(), shellOf("alice").fakeEnc())
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        val outputSlot = slot<Output>()
        fakeCryptoProvider(instance = cryptoProvider)

        passbirdEventRegistry.register(ProteinCreated(egg, egg.proteins[S1.index()].get()))
        passbirdEventRegistry.processEvents()

        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains "Protein 'user' for egg 'EggId' successfully created."
    }

    @Test
    fun `should process protein updated event when type stays the same`() {
        val egg = createEggForTesting(
            withEggIdShell = shellOf("EggId"),
            withProteins = mapOf(S1 to ShellPair(shellOf("user"), shellOf("alice"))),
        )
        val oldProtein = egg.proteins[S1.index()].get()
        egg.clearDomainEvents()
        egg.updateProtein(S1, shellOf("user").fakeEnc(), shellOf("bob").fakeEnc())
        val newProtein = egg.proteins[S1.index()].get()
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        val outputSlot = slot<Output>()
        fakeCryptoProvider(instance = cryptoProvider)

        passbirdEventRegistry.register(ProteinUpdated(egg, S1, oldProtein, newProtein))
        passbirdEventRegistry.processEvents()

        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains
            "Protein 'user' at slot 1 for egg 'EggId' successfully updated."
    }

    @Test
    fun `should process protein updated event when type changes`() {
        val egg = createEggForTesting(
            withEggIdShell = shellOf("EggId"),
            withProteins = mapOf(S1 to ShellPair(shellOf("login"), shellOf("alice"))),
        )
        val oldProtein = egg.proteins[S1.index()].get()
        egg.clearDomainEvents()
        egg.updateProtein(S1, shellOf("user").fakeEnc(), shellOf("bob").fakeEnc())
        val newProtein = egg.proteins[S1.index()].get()
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        val outputSlot = slot<Output>()
        fakeCryptoProvider(instance = cryptoProvider)

        passbirdEventRegistry.register(ProteinUpdated(egg, S1, oldProtein, newProtein))
        passbirdEventRegistry.processEvents()

        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains
            "Protein for egg 'EggId' at slot 1 successfully updated from 'login' to 'user'."
    }

    @Test
    fun `should process protein discarded event`() {
        val egg = createEggForTesting(
            withEggIdShell = shellOf("EggId"),
            withProteins = mapOf(S1 to ShellPair(shellOf("user"), shellOf("alice"))),
        )
        val protein = egg.proteins[S1.index()].get()
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        val outputSlot = slot<Output>()
        fakeCryptoProvider(instance = cryptoProvider)

        passbirdEventRegistry.register(ProteinDiscarded(egg, protein))
        passbirdEventRegistry.processEvents()

        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains
            "Protein 'user' of egg 'EggId' successfully discarded."
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
