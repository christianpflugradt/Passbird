package de.pflugradts.passbird.application.boot.main

import com.sun.jna.Pointer
import de.pflugradts.passbird.application.MacOsMainThreadBridge
import de.pflugradts.passbird.application.MacOsMainThreadDispatcher
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class MacOsMainThreadBridgeTest {
    @Test
    fun `should execute bridge work inline when no dispatcher is installed`() {
        val actual = MacOsMainThreadBridge.dispatch { "inline" }

        expectThat(actual).isEqualTo("inline")
    }

    @Test
    fun `should install main thread dispatcher before worker starts`() {
        val runtime = BridgeTestMacOsApplicationRuntime()
        val dispatcher = RecordingMacOsMainThreadDispatcher()

        MacOsApplicationLoopGraph(
            osName = "Mac OS X",
            startsOnFirstThread = true,
            runtimeFactory = { runtime },
            mainThreadDispatcherFactory = { dispatcher },
            startWorker = { work -> work() },
        ).run {
            MacOsMainThreadBridge.dispatch { dispatcher.events += "worker" }
        }

        expectThat(dispatcher.dispatchCalls).isEqualTo(1)
        expectThat(dispatcher.closeCalls).isEqualTo(1)
        expectThat(dispatcher.events).containsExactly("worker")
        expectThat(runtime.events).containsExactly("terminate", "run")
    }

    @Test
    fun `should ignore uninstall for a different dispatcher`() {
        val installedDispatcher = RecordingMacOsMainThreadDispatcher()
        val otherDispatcher = RecordingMacOsMainThreadDispatcher()

        MacOsMainThreadBridge.install(installedDispatcher)
        MacOsMainThreadBridge.uninstall(otherDispatcher)
        MacOsMainThreadBridge.dispatch { installedDispatcher.events += "still-installed" }
        MacOsMainThreadBridge.uninstall(installedDispatcher)

        expectThat(installedDispatcher.dispatchCalls).isEqualTo(1)
        expectThat(installedDispatcher.closeCalls).isEqualTo(1)
        expectThat(installedDispatcher.events).containsExactly("still-installed")
        expectThat(otherDispatcher.closeCalls).isEqualTo(0)
    }

    @Test
    fun `should dispatch queued work through core foundation run loop source`() {
        val coreFoundation = FakeMacOsCoreFoundation()
        val dispatcher = CoreFoundationMacOsMainThreadDispatcher(
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
        val coreFoundation = FakeMacOsCoreFoundation()
        val dispatcher = CoreFoundationMacOsMainThreadDispatcher(
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
        val coreFoundation = FakeMacOsCoreFoundation()
        val dispatcher = CoreFoundationMacOsMainThreadDispatcher(
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
}

private class BridgeTestMacOsApplicationRuntime(
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

private class RecordingMacOsMainThreadDispatcher : MacOsMainThreadDispatcher {
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

private class FakeMacOsCoreFoundation : MacOsCoreFoundation {
    val runLoop = Pointer(11)
    val mode = Pointer(12)
    val source = Pointer(13)
    val signaledSources = mutableListOf<Pointer>()
    val wokenRunLoops = mutableListOf<Pointer>()
    val invalidatedSources = mutableListOf<Pointer>()
    val removedSources = mutableListOf<Triple<Pointer, Pointer, Pointer>>()
    val releasedPointers = mutableListOf<Pointer>()
    var perform: MacOsCoreFoundation.RunLoopSourcePerformCallback? = null

    override fun CFRunLoopGetCurrent() = runLoop

    override fun CFRunLoopAddSource(runLoop: Pointer, source: Pointer, mode: Pointer) = Unit

    override fun CFRunLoopRemoveSource(runLoop: Pointer, source: Pointer, mode: Pointer) {
        removedSources += Triple(runLoop, source, mode)
    }

    override fun CFRunLoopSourceCreate(allocator: Pointer?, order: Int, context: MacOsRunLoopSourceContext): Pointer {
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
