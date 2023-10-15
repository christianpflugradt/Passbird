package de.pflugradts.passbird.application.eventhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.event.PasswordEntryCreated
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
import de.pflugradts.passbird.domain.model.event.PasswordEntryRenamed
import de.pflugradts.passbird.domain.model.event.PasswordEntryUpdated
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isTrue
import java.util.stream.Stream

class ApplicationEventHandlerIT {

    private val cryptoProvider = mockk<CryptoProvider>()
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>()
    private val applicationEventHandler = ApplicationEventHandler(cryptoProvider, userInterfaceAdapterPort)
    private var passbirdEventRegistry = PassbirdEventRegistry(mutableSetOf<EventHandler>(applicationEventHandler))

    @ParameterizedTest
    @MethodSource("providePasswordEvents")
    fun shouldProcessPasswordEntryCreated(domainEvent: DomainEvent) {
        // given
        val expectedBytes = bytesOf("expected key")
        every { cryptoProvider.decrypt(any(Bytes::class)) } answers { expectedBytes }
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        val outputSlot = slot<Output>()

        // when
        passbirdEventRegistry.register(domainEvent)
        passbirdEventRegistry.processEvents()

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.isCaptured).isTrue()
        expectThat(outputSlot.captured.bytes.asString()) contains expectedBytes.asString()
    }

    companion object {
        @JvmStatic
        private fun providePasswordEvents() = Stream.of(
            Arguments.of(PasswordEntryCreated(createPasswordEntryForTesting())),
            Arguments.of(PasswordEntryUpdated(createPasswordEntryForTesting())),
            Arguments.of(PasswordEntryRenamed(createPasswordEntryForTesting())),
            Arguments.of(PasswordEntryRenamed(createPasswordEntryForTesting())),
            Arguments.of(PasswordEntryNotFound(emptyBytes())),
        )
    }
}
