package de.pflugradts.passbird.application

import com.google.inject.Module
import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.launcher.LauncherModule
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

fun mockMain(moduleSlot: CapturingSlot<Module>? = null, withMockedFileCheck: Boolean = true) {
    mockkStatic(::bootModule)
    every { bootModule(if (moduleSlot != null) capture(moduleSlot) else any(LauncherModule::class)) } returns Unit
    if (withMockedFileCheck) mockkStatic(::checkHomeDirectory)
}

fun unmockMain(withMockedFileCheck: Boolean = true) {
    unmockkStatic(::bootModule)
    if (withMockedFileCheck) unmockkStatic(::checkHomeDirectory)
}

fun mainMocked(args: Array<String>, withMockedFileCheck: Boolean = true) {
    mockMain(withMockedFileCheck = withMockedFileCheck)
    main(args)
    unmockMain(withMockedFileCheck)
}
