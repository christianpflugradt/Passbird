package de.pflugradts.passbird.adapter.userinterface.hotkey.carbon

import com.sun.jna.Pointer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class CarbonMacOsRuntimeTest {
    @Test
    fun `should execute application directly when shared application is unavailable`() {
        var executions = 0

        CarbonMacOsApplicationLoopRunner(
            runtimeFactory = { TestCarbonMacOsApplicationRuntime(application = null) },
        ).run { executions++ }

        expectThat(executions).isEqualTo(1)
    }

    @Test
    fun `should install carbon runtime context before worker starts`() {
        val runtime = TestCarbonMacOsApplicationRuntime()
        val dispatcher = RecordingCarbonMacOsMainThreadDispatcher()

        CarbonMacOsApplicationLoopRunner(
            runtimeFactory = { runtime },
            mainThreadDispatcherFactory = { dispatcher },
            startWorker = { work -> work() },
        ).run {
            CarbonMacOsRuntimeContext.dispatch { dispatcher.events += "worker" }
        }

        expectThat(dispatcher.dispatchCalls).isEqualTo(1)
        expectThat(dispatcher.closeCalls).isEqualTo(1)
        expectThat(dispatcher.events).containsExactly("worker")
        expectThat(runtime.events).containsExactly("terminate", "run")
    }

    @Test
    fun `should ignore uninstall for a different dispatcher`() {
        val installedDispatcher = RecordingCarbonMacOsMainThreadDispatcher()
        val otherDispatcher = RecordingCarbonMacOsMainThreadDispatcher()

        CarbonMacOsRuntimeContext.install(installedDispatcher)
        CarbonMacOsRuntimeContext.uninstall(otherDispatcher)
        CarbonMacOsRuntimeContext.dispatch { installedDispatcher.events += "still-installed" }
        CarbonMacOsRuntimeContext.uninstall(installedDispatcher)

        expectThat(installedDispatcher.dispatchCalls).isEqualTo(1)
        expectThat(installedDispatcher.closeCalls).isEqualTo(1)
        expectThat(installedDispatcher.events).containsExactly("still-installed")
        expectThat(otherDispatcher.closeCalls).isEqualTo(0)
    }

    @Test
    fun `should dispatch queued work through core foundation run loop source`() {
        val coreFoundation = FakeCarbonMacOsCoreFoundation()
        val dispatcher = CoreFoundationCarbonMacOsMainThreadDispatcher(
            coreFoundation = coreFoundation,
            ownerThread = Thread.currentThread(),
        )
        val result = CompletableFuture<String>()

        val worker = Thread({
            result.complete(
                dispatcher.dispatch { "ok" },
            )
        }, "worker")
        worker.start()

        while (coreFoundation.signaledSources.isEmpty()) {
            Thread.sleep(10)
        }
        coreFoundation.perform?.callback(Pointer.NULL)
        worker.join(TimeUnit.SECONDS.toMillis(1))

        expectThat(result.get()).isEqualTo("ok")
        expectThat(coreFoundation.signaledSources).containsExactly(coreFoundation.source)
        expectThat(coreFoundation.wokenRunLoops).containsExactly(coreFoundation.runLoop)

        dispatcher.close()

        expectThat(coreFoundation.invalidatedSources).containsExactly(coreFoundation.source)
        expectThat(coreFoundation.removedSources).containsExactly(
            Triple(coreFoundation.runLoop, coreFoundation.source, coreFoundation.mode),
        )
        expectThat(coreFoundation.releasedPointers).containsExactly(coreFoundation.source, coreFoundation.mode)
    }

    @Test
    fun `should execute dispatcher work inline on owner thread`() {
        val coreFoundation = FakeCarbonMacOsCoreFoundation()
        val dispatcher = CoreFoundationCarbonMacOsMainThreadDispatcher(
            coreFoundation = coreFoundation,
            ownerThread = Thread.currentThread(),
        )

        val actual = dispatcher.dispatch { "owner-thread" }

        expectThat(actual).isEqualTo("owner-thread")
        expectThat(coreFoundation.signaledSources).containsExactly()
        expectThat(coreFoundation.wokenRunLoops).containsExactly()

        dispatcher.close()
    }

    @Test
    fun `should execute dispatcher work inline after close`() {
        val coreFoundation = FakeCarbonMacOsCoreFoundation()
        val dispatcher = CoreFoundationCarbonMacOsMainThreadDispatcher(
            coreFoundation = coreFoundation,
            ownerThread = Thread.currentThread(),
        )
        dispatcher.close()

        val result = CompletableFuture<String>()
        val worker = Thread({
            result.complete(
                dispatcher.dispatch { "closed" },
            )
        }, "worker")
        worker.start()
        worker.join(TimeUnit.SECONDS.toMillis(1))

        expectThat(result.get()).isEqualTo("closed")
        expectThat(coreFoundation.signaledSources).containsExactly()
        expectThat(coreFoundation.wokenRunLoops).containsExactly()
        expectThat(coreFoundation.invalidatedSources).containsExactly(coreFoundation.source)
    }

    @Test
    fun `should fail closed when carbon runtime context is unavailable`() {
        val actual = assertThrows<IllegalStateException> {
            CarbonMacOsRuntimeContextExecutor.dispatch { error("boom") }
        }

        expectThat(actual.message).isEqualTo("Carbon macOS main thread runtime is not active")
    }

    @Test
    fun `should return null when objective c application class is unavailable`() {
        val objectiveC = FakeCarbonObjectiveC(applicationClass = null)

        val actual = ObjectiveCCarbonMacOsApplicationRuntime(objectiveC).sharedApplication()

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should resolve shared application through objective c runtime`() {
        val applicationClass = Pointer(2)
        val selector = Pointer(3)
        val application = Pointer(4)
        val objectiveC = FakeCarbonObjectiveC(
            applicationClass = applicationClass,
            registeredSelector = selector,
            sharedApplication = application,
        )

        val actual = ObjectiveCCarbonMacOsApplicationRuntime(objectiveC).sharedApplication()

        expectThat(actual).isEqualTo(application)
        expectThat(objectiveC.messages).containsExactly(
            "objc_getClass:NSApplication",
            "sel_registerName:sharedApplication",
            "objc_msgSendPointer:2:3",
        )
    }
}

private class TestCarbonMacOsApplicationRuntime(
    private val application: Pointer? = Pointer(1),
) : CarbonMacOsApplicationRuntime {
    val events = mutableListOf<String>()

    override fun sharedApplication() = application

    override fun run(application: Pointer) {
        events += "run"
    }

    override fun terminate(application: Pointer) {
        events += "terminate"
    }
}

private class RecordingCarbonMacOsMainThreadDispatcher : CarbonMacOsMainThreadDispatcher {
    val events = mutableListOf<String>()
    var dispatchCalls = 0
    var closeCalls = 0

    override fun <T> dispatch(work: () -> T): T {
        dispatchCalls++
        return work()
    }

    override fun close() {
        closeCalls++
    }
}

private class FakeCarbonMacOsCoreFoundation : CarbonMacOsCoreFoundation {
    val runLoop = Pointer(11)
    val mode = Pointer(12)
    val source = Pointer(13)
    val signaledSources = mutableListOf<Pointer>()
    val wokenRunLoops = mutableListOf<Pointer>()
    val invalidatedSources = mutableListOf<Pointer>()
    val removedSources = mutableListOf<Triple<Pointer, Pointer, Pointer>>()
    val releasedPointers = mutableListOf<Pointer>()
    var perform: CarbonMacOsCoreFoundation.RunLoopSourcePerformCallback? = null

    override fun CFRunLoopGetCurrent() = runLoop

    override fun CFRunLoopAddSource(runLoop: Pointer, source: Pointer, mode: Pointer) = Unit

    override fun CFRunLoopRemoveSource(runLoop: Pointer, source: Pointer, mode: Pointer) {
        removedSources += Triple(runLoop, source, mode)
    }

    override fun CFRunLoopSourceCreate(allocator: Pointer?, order: Int, context: CarbonMacOsRunLoopSourceContext): Pointer {
        perform = context.perform
        return source
    }

    override fun CFRunLoopSourceSignal(source: Pointer) {
        signaledSources += source
    }

    override fun CFRunLoopSourceInvalidate(source: Pointer) {
        invalidatedSources += source
    }

    override fun CFRunLoopWakeUp(runLoop: Pointer) {
        wokenRunLoops += runLoop
    }

    override fun CFStringCreateWithCString(allocator: Pointer?, value: String, encoding: Int) = mode

    override fun CFRelease(reference: Pointer?) {
        reference?.let(releasedPointers::add)
    }
}

private class FakeCarbonObjectiveC(
    private val applicationClass: Pointer? = Pointer(1),
    private val registeredSelector: Pointer = Pointer(2),
    private val sharedApplication: Pointer? = Pointer(3),
) : CarbonObjectiveC {
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
