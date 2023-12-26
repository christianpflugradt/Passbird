package de.pflugradts.passbird.application

import com.google.inject.Module
import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.launcher.LauncherModule
import de.pflugradts.passbird.application.util.SystemOperation
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

fun mockMain(
    moduleSlot: CapturingSlot<Module>? = null,
    systemOperationMock: SystemOperation = mockk<SystemOperation>(),
    withMockedFileCheck: Boolean = true,
) {
    mockkStatic(::bootModule)
    every { bootModule(if (moduleSlot != null) capture(moduleSlot) else any(LauncherModule::class)) } returns Unit
    mockkStatic(::mainGetSystemOperation)
    every { mainGetSystemOperation() } returns systemOperationMock
    if (withMockedFileCheck) {
        mockkStatic(::mainCheckHomeDirectory)
        every { mainCheckHomeDirectory(any()) } returns Unit
    }
    every { systemOperationMock.exit() } returns Unit
}

fun unmockMain(withMockedFileCheck: Boolean = true) {
    unmockkStatic(::bootModule)
    unmockkStatic(::mainGetSystemOperation)
    if (withMockedFileCheck) unmockkStatic(::mainCheckHomeDirectory)
}

fun mainMocked(args: Array<String>, withMockedFileCheck: Boolean = true) {
    mockMain(withMockedFileCheck = withMockedFileCheck)
    main(args)
    unmockMain(withMockedFileCheck)
}
