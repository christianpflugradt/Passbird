package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.launcher.LauncherModule
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

fun mockMain() {
    System.clearProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY)
    mockkStatic(::bootModule)
    every { bootModule(any(LauncherModule::class)) } returns Unit
}

fun unmockMain() {
    unmockkStatic(::bootModule)
}
