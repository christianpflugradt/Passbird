@file:Suppress("ktlint:standard:function-naming")

package de.pflugradts.passbird.application.boot.main

import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import de.pflugradts.passbird.application.GlobalHotkeyBackend
import de.pflugradts.passbird.application.MacOsMainThreadBridge
import de.pflugradts.passbird.application.MacOsMainThreadDispatcher
import java.lang.management.ManagementFactory
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

internal class MacOsApplicationLoopGraph(
    private val osName: String = System.getProperty("os.name").orEmpty(),
    private val startsOnFirstThread: Boolean = ManagementFactory.getRuntimeMXBean()
        .inputArguments
        .contains("-XstartOnFirstThread"),
    private val globalHotkeyEnabled: Boolean = true,
    private val globalHotkeyBackend: GlobalHotkeyBackend = GlobalHotkeyBackend.AUTO,
    private val runtimeFactory: () -> MacOsApplicationRuntime = ::ObjectiveCMacOsApplicationRuntime,
    private val mainThreadDispatcherFactory: () -> MacOsMainThreadDispatcher = ::CoreFoundationMacOsMainThreadDispatcher,
    private val startWorker: ((() -> Unit) -> Unit) = { work ->
        Thread(work, "passbird-cli").start()
    },
) {
    fun run(application: () -> Unit) {
        if (!requiresMacOsApplicationLoop()) {
            application()
            return
        }
        val runtime = runtimeFactory()
        val macOsApplication = runtime.sharedApplication()
        if (macOsApplication == null) {
            application()
            return
        }
        val mainThreadDispatcher = mainThreadDispatcherFactory()
        MacOsMainThreadBridge.install(mainThreadDispatcher)
        try {
            startWorker {
                try {
                    application()
                } finally {
                    runtime.terminate(macOsApplication)
                }
            }
            runtime.run(macOsApplication)
        } finally {
            MacOsMainThreadBridge.uninstall(mainThreadDispatcher)
        }
    }

    private fun requiresMacOsApplicationLoop() = osName.lowercase().contains("mac") &&
        startsOnFirstThread &&
        globalHotkeyEnabled &&
        globalHotkeyBackend.resolvePolicy(osName).requiresMacOsApplicationLoop(startsOnFirstThread)
}

internal interface MacOsApplicationRuntime {
    fun sharedApplication(): Pointer?
    fun run(application: Pointer)
    fun terminate(application: Pointer)
}

internal class CoreFoundationMacOsMainThreadDispatcher(
    private val coreFoundation: MacOsCoreFoundation = MacOsCoreFoundation.instance(),
    private val ownerThread: Thread = Thread.currentThread(),
) : MacOsMainThreadDispatcher {
    private val tasks = ConcurrentLinkedQueue<MainThreadTask<*>>()
    private val closed = AtomicBoolean(false)
    private val perform = MacOsCoreFoundation.RunLoopSourcePerformCallback {
        while (true) {
            val task = tasks.poll() ?: return@RunLoopSourcePerformCallback
            task.run()
        }
    }
    private val runLoop = coreFoundation.CFRunLoopGetCurrent()
    private val mode = checkNotNull(
        coreFoundation.CFStringCreateWithCString(Pointer.NULL, CF_RUN_LOOP_DEFAULT_MODE, CF_STRING_ENCODING_UTF8),
    )
    private val sourceContext = MacOsRunLoopSourceContext().apply {
        version = 0
        info = Pointer.NULL
        perform = this@CoreFoundationMacOsMainThreadDispatcher.perform
        write()
    }
    private val source = checkNotNull(
        coreFoundation.CFRunLoopSourceCreate(Pointer.NULL, 0, sourceContext),
    )

    init {
        coreFoundation.CFRunLoopAddSource(runLoop, source, mode)
    }

    override fun <T> dispatch(work: () -> T): T {
        if (Thread.currentThread() === ownerThread || closed.get()) {
            return work()
        }
        val task = MainThreadTask(work)
        tasks.add(task)
        coreFoundation.CFRunLoopSourceSignal(source)
        coreFoundation.CFRunLoopWakeUp(runLoop)
        return task.await()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            coreFoundation.CFRunLoopSourceInvalidate(source)
            coreFoundation.CFRunLoopRemoveSource(runLoop, source, mode)
            coreFoundation.CFRelease(source)
            coreFoundation.CFRelease(mode)
        }
    }

    private class MainThreadTask<T>(
        private val work: () -> T,
    ) {
        private val completion = CountDownLatch(1)
        private var result: Result<T>? = null

        fun run() {
            result = runCatching(work)
            completion.countDown()
        }

        fun await(): T {
            completion.await()
            return requireNotNull(result).getOrThrow()
        }
    }
}

internal interface MacOsCoreFoundation : Library {
    fun CFRunLoopGetCurrent(): Pointer
    fun CFRunLoopAddSource(runLoop: Pointer, source: Pointer, mode: Pointer)
    fun CFRunLoopRemoveSource(runLoop: Pointer, source: Pointer, mode: Pointer)
    fun CFRunLoopSourceCreate(allocator: Pointer?, order: Int, context: MacOsRunLoopSourceContext): Pointer?
    fun CFRunLoopSourceSignal(source: Pointer)
    fun CFRunLoopSourceInvalidate(source: Pointer)
    fun CFRunLoopWakeUp(runLoop: Pointer)
    fun CFStringCreateWithCString(allocator: Pointer?, value: String, encoding: Int): Pointer?
    fun CFRelease(reference: Pointer?)

    fun interface RunLoopSourcePerformCallback : com.sun.jna.Callback {
        fun callback(info: Pointer?)
    }

    companion object {
        fun instance(): MacOsCoreFoundation = Native.load("CoreFoundation", MacOsCoreFoundation::class.java)
    }
}

internal class MacOsRunLoopSourceContext : com.sun.jna.Structure() {
    @JvmField
    var version = 0L

    @JvmField
    var info: Pointer? = Pointer.NULL

    @JvmField
    var retain: Pointer? = Pointer.NULL

    @JvmField
    var release: Pointer? = Pointer.NULL

    @JvmField
    var copyDescription: Pointer? = Pointer.NULL

    @JvmField
    var equal: Pointer? = Pointer.NULL

    @JvmField
    var hash: Pointer? = Pointer.NULL

    @JvmField
    var schedule: Pointer? = Pointer.NULL

    @JvmField
    var cancel: Pointer? = Pointer.NULL

    @JvmField
    internal var perform: MacOsCoreFoundation.RunLoopSourcePerformCallback? = null

    override fun getFieldOrder() = listOf(
        "version",
        "info",
        "retain",
        "release",
        "copyDescription",
        "equal",
        "hash",
        "schedule",
        "cancel",
        "perform",
    )
}

internal class ObjectiveCMacOsApplicationRuntime(
    private val objectiveC: ObjectiveC = ObjectiveC.instance(),
) : MacOsApplicationRuntime {
    override fun sharedApplication(): Pointer? {
        val applicationClass = objectiveC.objc_getClass("NSApplication") ?: return null
        return objectiveC.objc_msgSendPointer(
            applicationClass,
            objectiveC.sel_registerName("sharedApplication"),
        )
    }

    override fun run(application: Pointer) {
        objectiveC.objc_msgSendVoid(application, objectiveC.sel_registerName("run"))
    }

    override fun terminate(application: Pointer) {
        objectiveC.objc_msgSendVoidWithSelectorObjectAndBoolean(
            application,
            objectiveC.sel_registerName("performSelectorOnMainThread:withObject:waitUntilDone:"),
            objectiveC.sel_registerName("terminate:"),
            Pointer.NULL,
            false,
        )
    }
}

internal interface ObjectiveC : Library {
    fun objc_getClass(name: String): Pointer?
    fun sel_registerName(name: String): Pointer
    fun objc_msgSendPointer(receiver: Pointer, selector: Pointer): Pointer?
    fun objc_msgSendVoid(receiver: Pointer, selector: Pointer)
    fun objc_msgSendVoidWithSelectorObjectAndBoolean(
        receiver: Pointer,
        selector: Pointer,
        selectorArgument: Pointer,
        objectArgument: Pointer?,
        waitUntilDone: Boolean,
    )

    companion object {
        fun instance(): ObjectiveC = Native.load(
            "/usr/lib/libobjc.A.dylib",
            ObjectiveC::class.java,
            mapOf(
                Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _: NativeLibrary, method: Method ->
                    if (method.name.startsWith("objc_msgSend")) "objc_msgSend" else method.name
                },
            ),
        )
    }
}

private const val CF_RUN_LOOP_DEFAULT_MODE = "kCFRunLoopDefaultMode"
private const val CF_STRING_ENCODING_UTF8 = 0x08000100
