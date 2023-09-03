package de.pflugradts.passbird.application.util

import io.mockk.every

fun fakeSystemOperation(
    instance: SystemOperation,
    withConsoleEnabled: Boolean = true,
    withPasswordFromConsole: CharArray = CharArray(0),
) {
    every { instance.isConsoleAvailable } returns withConsoleEnabled
    every { instance.readPasswordFromConsole() } returns withPasswordFromConsole
}
