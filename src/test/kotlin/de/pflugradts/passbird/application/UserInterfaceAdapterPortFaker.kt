package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output
import io.mockk.every
import java.util.concurrent.atomic.AtomicInteger

fun fakeUserInterfaceAdapterPort(
    instance: UserInterfaceAdapterPort,
    withReceiveConfirmation: Boolean = false,
    withReceiveYes: Boolean = false,
    withTheseInputs: List<Input> = emptyList(),
    withTheseSecureInputs: List<Input> = emptyList(),
) {
    val inputCount = AtomicInteger(0)
    every { instance.receive() } answers { withTheseInputs[inputCount.getAndIncrement()] }
    every { instance.receive(*anyVararg<Output>()) } answers { withTheseInputs[inputCount.getAndIncrement()] }
    val secureInputCount = AtomicInteger(0)
    every { instance.receiveSecurely() } answers { withTheseSecureInputs[secureInputCount.getAndIncrement()] }
    every { instance.receiveSecurely(any<Output>()) } answers { withTheseSecureInputs[secureInputCount.getAndIncrement()] }
    every { instance.receiveConfirmation(any<Output>()) } returns withReceiveConfirmation
    every { instance.receiveYes(any<Output>()) } returns withReceiveYes
    every { instance.send(any()) } returns Unit
    every { instance.sendLineBreak() } returns Unit
    every { instance.warningSound() } returns Unit
}
