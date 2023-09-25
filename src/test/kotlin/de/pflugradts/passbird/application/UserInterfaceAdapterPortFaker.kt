package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output
import io.mockk.every
import java.util.concurrent.atomic.AtomicInteger

fun fakeUserInterfaceAdapterPort(
    instance: UserInterfaceAdapterPort,
    withTheseInputs: List<Input> = emptyList(),
    withTheseSecureInputs: List<Input> = emptyList(),
) {
    val inputCount = AtomicInteger(0)
    every { instance.receive() } answers { withTheseInputs[inputCount.getAndIncrement()] }
    every { instance.receive(any(Output::class)) } answers { withTheseInputs[inputCount.getAndIncrement()] }
    val secureInputCount = AtomicInteger(0)
    every { instance.receiveSecurely() } answers { withTheseSecureInputs[secureInputCount.getAndIncrement()] }
    every { instance.receiveSecurely(any(Output::class)) } answers { withTheseSecureInputs[secureInputCount.getAndIncrement()] }
    every { instance.send(any()) } returns Unit
    every { instance.sendLineBreak() } returns Unit
}
