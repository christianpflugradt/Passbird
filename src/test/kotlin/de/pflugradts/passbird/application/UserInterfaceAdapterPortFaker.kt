package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output
import io.mockk.every
import java.util.concurrent.atomic.AtomicInteger

fun fakeUserInterfaceAdapterPort(
    instance: UserInterfaceAdapterPort,
    withTheseSecureInputs: List<Input> = emptyList(),
) {
    val secureInputCount = AtomicInteger(0)
    every { instance.receiveSecurely() } answers { withTheseSecureInputs[secureInputCount.getAndIncrement()] }
    every { instance.receiveSecurely(any(Output::class)) } answers { withTheseSecureInputs[secureInputCount.getAndIncrement()] }
    every { instance.send(any()) } returns Unit
}
