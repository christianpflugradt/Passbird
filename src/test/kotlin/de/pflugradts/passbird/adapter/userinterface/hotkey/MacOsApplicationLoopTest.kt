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

    @Test
    fun `should return null when objective c application class is unavailable`() {
        val objectiveC = FakeObjectiveC(applicationClass = null)

        val actual = ObjectiveCMacOsApplicationRuntime(objectiveC).sharedApplication()

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should resolve shared application through objective c runtime`() {
        val applicationClass = Pointer(2)
        val selector = Pointer(3)
        val application = Pointer(4)
        val objectiveC = FakeObjectiveC(
            applicationClass = applicationClass,
            registeredSelector = selector,
            sharedApplication = application,
        )

        val actual = ObjectiveCMacOsApplicationRuntime(objectiveC).sharedApplication()

        expectThat(actual).isEqualTo(application)
        expectThat(objectiveC.messages).containsExactly(
            "objc_getClass:NSApplication",
            "sel_registerName:sharedApplication",
            "objc_msgSendPointer:2:3",
        )
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

private class FakeObjectiveC(
    private val applicationClass: Pointer? = Pointer(1),
    private val registeredSelector: Pointer = Pointer(2),
    private val sharedApplication: Pointer? = Pointer(3),
) : ObjectiveC {
    val messages = mutableListOf<String>()

    override fun objc_getClass(name: String): Pointer? {
        messages += "objc_getClass:$name"
        return applicationClass
    }

    override fun sel_registerName(name: String): Pointer {
        messages += "sel_registerName:$name"
        return registeredSelector
    }

    override fun objc_msgSendPointer(receiver: Pointer, selector: Pointer): Pointer? {
        messages += "objc_msgSendPointer:${Pointer.nativeValue(receiver)}:${Pointer.nativeValue(selector)}"
        return sharedApplication
    }

    override fun objc_msgSendVoid(receiver: Pointer, selector: Pointer) = Unit

    override fun objc_msgSendVoidWithSelectorObjectAndBoolean(
        receiver: Pointer,
        selector: Pointer,
        selectorArgument: Pointer,
        objectArgument: Pointer?,
        waitUntilDone: Boolean,
    ) = Unit
}
