@file:Suppress("ktlint:standard:function-naming")

package de.pflugradts.passbird.adapter.userinterface.hotkey.carbon

import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import de.pflugradts.passbird.application.boot.main.MacOsApplicationLoopRunner
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class CarbonMacOsApplicationLoopRunner(
    private val runtimeFactory: () -> CarbonMacOsApplicationRuntime = ::ObjectiveCCarbonMacOsApplicationRuntime,
    private val mainThreadDispatcherFactory: () -> CarbonMacOsMainThreadDispatcher =
        ::CoreFoundationCarbonMacOsMainThreadDispatcher,
    private val startWorker: ((() -> Unit) -> Unit) = { work ->
        Thread(work, "passbird-cli").start()
    },
) : MacOsApplicationLoopRunner {
    override fun run(application: () -> Unit) {
        val runtime = runtimeFactory()
        val macOsApplication = runtime.sharedApplication()
        if (macOsApplication == null) {
            application()
            return
        }
        val mainThreadDispatcher = mainThreadDispatcherFactory()
        CarbonMacOsRuntimeContext.install(mainThreadDispatcher)
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
            CarbonMacOsRuntimeContext.uninstall(mainThreadDispatcher)
        }
    }
}

internal interface CarbonMacOsApplicationRuntime {
    fun sharedApplication(): Pointer?
    fun run(application: Pointer)
    fun terminate(application: Pointer)
}

internal interface CarbonMacOsMainThreadExecutor {
    fun <T> dispatch(work: () -> T): T
}

internal interface CarbonMacOsMainThreadDispatcher : CarbonMacOsMainThreadExecutor {
    fun close()
}

internal object CarbonMacOsRuntimeContext {
    private val dispatcher = AtomicReference<CarbonMacOsMainThreadDispatcher?>(null)

    fun install(dispatcher: CarbonMacOsMainThreadDispatcher) {
        this.dispatcher.set(dispatcher)
    }

    fun uninstall(dispatcher: CarbonMacOsMainThreadDispatcher) {
        if (this.dispatcher.compareAndSet(dispatcher, null)) {
            dispatcher.close()
        }
    }

    fun <T> dispatch(work: () -> T): T? = dispatcher.get()?.dispatch(work)
}

internal class CoreFoundationCarbonMacOsMainThreadDispatcher(
    private val coreFoundation: CarbonMacOsCoreFoundation = CarbonMacOsCoreFoundation.instance(),
    private val ownerThread: Thread = Thread.currentThread(),
) : CarbonMacOsMainThreadDispatcher {
    private val tasks = ConcurrentLinkedQueue<MainThreadTask<*>>()
    private val closed = AtomicBoolean(false)
    private val perform = CarbonMacOsCoreFoundation.RunLoopSourcePerformCallback {
        while (true) {
            val task = tasks.poll() ?: return@RunLoopSourcePerformCallback
            task.run()
        }
    }
    private val runLoop = coreFoundation.CFRunLoopGetCurrent()
    private val defaultMode = checkNotNull(
        coreFoundation.CFStringCreateWithCString(Pointer.NULL, CF_RUN_LOOP_DEFAULT_MODE, CF_STRING_ENCODING_UTF8),
    )
    private val commonModes = checkNotNull(
        coreFoundation.CFStringCreateWithCString(Pointer.NULL, CF_RUN_LOOP_COMMON_MODES, CF_STRING_ENCODING_UTF8),
    )
    private val sourceContext = CarbonMacOsRunLoopSourceContext().apply {
        version = 0
        info = Pointer.NULL
        perform = this@CoreFoundationCarbonMacOsMainThreadDispatcher.perform
        write()
    }
    private val source = checkNotNull(
        coreFoundation.CFRunLoopSourceCreate(Pointer.NULL, 0, sourceContext),
    )

    init {
        coreFoundation.CFRunLoopAddSource(runLoop, source, defaultMode)
        coreFoundation.CFRunLoopAddSource(runLoop, source, commonModes)
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
            coreFoundation.CFRunLoopRemoveSource(runLoop, source, defaultMode)
            coreFoundation.CFRunLoopRemoveSource(runLoop, source, commonModes)
            coreFoundation.CFRelease(source)
            coreFoundation.CFRelease(defaultMode)
            coreFoundation.CFRelease(commonModes)
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

internal interface CarbonMacOsCoreFoundation : Library {
    fun CFRunLoopGetCurrent(): Pointer
    fun CFRunLoopAddSource(runLoop: Pointer, source: Pointer, mode: Pointer)
    fun CFRunLoopRemoveSource(runLoop: Pointer, source: Pointer, mode: Pointer)
    fun CFRunLoopSourceCreate(allocator: Pointer?, order: Int, context: CarbonMacOsRunLoopSourceContext): Pointer?
    fun CFRunLoopSourceSignal(source: Pointer)
    fun CFRunLoopSourceInvalidate(source: Pointer)
    fun CFRunLoopWakeUp(runLoop: Pointer)
    fun CFStringCreateWithCString(allocator: Pointer?, value: String, encoding: Int): Pointer?
    fun CFRelease(reference: Pointer?)

    fun interface RunLoopSourcePerformCallback : com.sun.jna.Callback {
        fun callback(info: Pointer?)
    }

    companion object {
        fun instance(): CarbonMacOsCoreFoundation = Native.load("CoreFoundation", CarbonMacOsCoreFoundation::class.java)
    }
}

internal class CarbonMacOsRunLoopSourceContext : com.sun.jna.Structure() {
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
    internal var perform: CarbonMacOsCoreFoundation.RunLoopSourcePerformCallback? = null

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

internal class ObjectiveCCarbonMacOsApplicationRuntime(
    private val objectiveC: CarbonObjectiveC = CarbonObjectiveC.instance(),
) : CarbonMacOsApplicationRuntime {
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

internal interface CarbonObjectiveC : Library {
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
        fun instance(): CarbonObjectiveC = Native.load(
            "/usr/lib/libobjc.A.dylib",
            CarbonObjectiveC::class.java,
            mapOf(
                Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _: NativeLibrary, method: Method ->
                    if (method.name.startsWith("objc_msgSend")) "objc_msgSend" else method.name
                },
            ),
        )
    }
}

private const val CF_RUN_LOOP_DEFAULT_MODE = "kCFRunLoopDefaultMode"
private const val CF_RUN_LOOP_COMMON_MODES = "kCFRunLoopCommonModes"
private const val CF_STRING_ENCODING_UTF8 = 0x08000100
