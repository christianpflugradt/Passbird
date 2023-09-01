package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.application.configuration.Configuration.Adapter
import de.pflugradts.passbird.application.configuration.Configuration.Clipboard
import de.pflugradts.passbird.application.configuration.Configuration.ClipboardReset
import io.mockk.every
import io.mockk.mockk

fun fakeConfiguration(
    instance: Configuration,
    clipboardResetEnabled: Boolean = false,
    clipboardResetDelaySeconds: Int = 0,
) {
    val clipboardReset = mockk<ClipboardReset>()
    every { clipboardReset.isEnabled } returns clipboardResetEnabled
    every { clipboardReset.delaySeconds } returns clipboardResetDelaySeconds
    val clipboard = mockk<Clipboard>()
    every { clipboard.reset } returns clipboardReset
    val adapter = mockk<Adapter>()
    every { adapter.clipboard } returns clipboard
    every { instance.adapter } returns adapter
}
