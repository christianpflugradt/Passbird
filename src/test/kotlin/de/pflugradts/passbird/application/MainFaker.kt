package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.util.SystemOperation
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

private const val MAIN_KT = "de.pflugradts.passbird.application.MainKt"

fun mockMain(
    runContextSlot: CapturingSlot<RunContext>? = null,
    systemOperationMock: SystemOperation = mockk<SystemOperation>(),
    withMockedFileCheck: Boolean = true,
) {
    mockkStatic(MAIN_KT)
    every { mainBootLauncher(if (runContextSlot != null) capture(runContextSlot) else any()) } returns Unit
    every { mainGetSystemOperation() } returns systemOperationMock
    if (withMockedFileCheck) {
        every { mainHasValidHomeDirectory(any()) } returns true
    }
    every { systemOperationMock.exit(any()) } returns Unit
}

fun unmockMain() {
    unmockkStatic(MAIN_KT)
}

fun mainMocked(args: Array<String>, withMockedFileCheck: Boolean = true) {
    mockMain(withMockedFileCheck = withMockedFileCheck)
    main(args)
    unmockMain()
}
