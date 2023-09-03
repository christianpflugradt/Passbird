package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.application.configuration.Configuration.Adapter
import de.pflugradts.passbird.application.configuration.Configuration.Clipboard
import de.pflugradts.passbird.application.configuration.Configuration.ClipboardReset
import de.pflugradts.passbird.application.configuration.Configuration.UserInterface
import io.mockk.every
import io.mockk.mockk

fun fakeConfiguration(
    instance: Configuration,
    withClipboardResetEnabled: Boolean = false,
    withClipboardResetDelaySeconds: Int = 0,
    withSecureInputEnabled: Boolean = true,
) {
    val clipboardReset = mockk<ClipboardReset>()
    every { clipboardReset.isEnabled } returns withClipboardResetEnabled
    every { clipboardReset.delaySeconds } returns withClipboardResetDelaySeconds
    val clipboard = mockk<Clipboard>()
    every { clipboard.reset } returns clipboardReset
    val userInterface = mockk<UserInterface>()
    every { userInterface.isSecureInput } returns withSecureInputEnabled
    val adapter = mockk<Adapter>()
    every { adapter.clipboard } returns clipboard
    every { adapter.userInterface } returns userInterface
    every { instance.adapter } returns adapter
}
