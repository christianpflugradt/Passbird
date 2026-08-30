package de.pflugradts.passbird.application.boot.main

import com.sun.jna.Pointer
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

class MacOsApplicationLoopTest {
    @Test
    fun `should execute application directly outside mac os`() {
        var executions = 0

        MacOsApplicationLoopGraph(
            osName = "Linux",
            runtimeFactory = { error("mac os runtime must not be created") },
        ).run { executions++ }

        expectThat(executions).isEqualTo(1)
    }

    @Test
    fun `should execute application directly without mac os first thread startup`() {
        var executions = 0

        MacOsApplicationLoopGraph(
            osName = "Mac OS X",
            startsOnFirstThread = false,
            runtimeFactory = { error("mac os runtime must not be created") },
        ).run { executions++ }

        expectThat(executions).isEqualTo(1)
    }

    @Test
    fun `should execute application directly when global hotkey is disabled`() {
        var executions = 0

        MacOsApplicationLoopGraph(
            osName = "Mac OS X",
            startsOnFirstThread = true,
            globalHotkeyEnabled = false,
            runtimeFactory = { error("mac os runtime must not be created") },
        ).run { executions++ }

        expectThat(executions).isEqualTo(1)
    }

    @Test
    fun `should execute application directly when carbon is not selected`() {
        var executions = 0

        MacOsApplicationLoopGraph(
            osName = "Mac OS X",
            startsOnFirstThread = true,
            globalHotkeyBackend = "quartz",
            runtimeFactory = { error("mac os runtime must not be created") },
        ).run { executions++ }

        expectThat(executions).isEqualTo(1)
    }

    @Test
    fun `should execute application directly when mac os application is unavailable`() {
        var executions = 0
        val runtime = FakeMacOsApplicationRuntime(application = null)

        MacOsApplicationLoopGraph(
            osName = "Mac OS X",
            startsOnFirstThread = true,
            runtimeFactory = { runtime },
        ).run { executions++ }

        expectThat(executions).isEqualTo(1)
        expectThat(runtime.events).isEmpty()
    }

    @Test
    fun `should run application on worker while main thread runs mac os event loop`() {
        val runtime = FakeMacOsApplicationRuntime()
        var executions = 0

        MacOsApplicationLoopGraph(
            osName = "Mac OS X",
            startsOnFirstThread = true,
            runtimeFactory = { runtime },
            startWorker = { work -> work() },
        ).run { executions++ }

        expectThat(executions).isEqualTo(1)
        expectThat(runtime.events).containsExactly("terminate", "run")
    }
}

private class FakeMacOsApplicationRuntime(
    private val application: Pointer? = Pointer(1),
) : MacOsApplicationRuntime {
    val events = mutableListOf<String>()

    override fun sharedApplication() = application

    override fun run(application: Pointer) {
        events += "run"
    }

    override fun terminate(application: Pointer) {
        events += "terminate"
    }
}
