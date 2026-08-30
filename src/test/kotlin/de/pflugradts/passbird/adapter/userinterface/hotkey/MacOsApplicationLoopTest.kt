package de.pflugradts.passbird.application.boot.main

import de.pflugradts.passbird.application.GlobalHotkeyBackend
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class MacOsApplicationLoopTest {
    @Test
    fun `should execute application directly outside mac os`() {
        var executions = 0

        MacOsApplicationLoopGraph(
            osName = "Linux",
        ).run { executions++ }

        expectThat(executions).isEqualTo(1)
    }

    @Test
    fun `should execute application directly when global hotkey is disabled`() {
        var executions = 0

        MacOsApplicationLoopGraph(
            osName = "Mac OS X",
            globalHotkeyEnabled = false,
        ).run { executions++ }

        expectThat(executions).isEqualTo(1)
    }

    @Test
    fun `should execute application directly when carbon is not selected`() {
        var executions = 0

        MacOsApplicationLoopGraph(
            osName = "Mac OS X",
            globalHotkeyBackend = GlobalHotkeyBackend.QUARTZ,
        ).run { executions++ }

        expectThat(executions).isEqualTo(1)
    }

    @Test
    fun `should delegate to injected runner only for carbon backend`() {
        var executions = 0
        var runnerCalls = 0

        MacOsApplicationLoopGraph(
            osName = "Mac OS X",
            globalHotkeyBackend = GlobalHotkeyBackend.CARBON,
            applicationLoopRunner = MacOsApplicationLoopRunner {
                runnerCalls++
                it()
            },
        ).run { executions++ }

        expectThat(runnerCalls).isEqualTo(1)
        expectThat(executions).isEqualTo(1)
    }
}
