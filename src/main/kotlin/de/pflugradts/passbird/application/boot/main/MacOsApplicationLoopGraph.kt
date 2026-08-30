@file:Suppress("ktlint:standard:function-naming")

package de.pflugradts.passbird.application.boot.main

import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.lang.management.ManagementFactory
import java.lang.reflect.Method

internal class MacOsApplicationLoopGraph(
    private val osName: String = System.getProperty("os.name").orEmpty(),
    private val startsOnFirstThread: Boolean = ManagementFactory.getRuntimeMXBean()
        .inputArguments
        .contains("-XstartOnFirstThread"),
    private val globalHotkeyEnabled: Boolean = true,
    private val globalHotkeyBackend: String = "auto",
    private val runtimeFactory: () -> MacOsApplicationRuntime = ::ObjectiveCMacOsApplicationRuntime,
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
        startWorker {
            try {
                application()
            } finally {
                runtime.terminate(macOsApplication)
            }
        }
        runtime.run(macOsApplication)
    }

    private fun requiresMacOsApplicationLoop() = osName.lowercase().contains("mac") &&
        startsOnFirstThread &&
        globalHotkeyEnabled &&
        globalHotkeyBackend in setOf("auto", "carbon")
}

internal interface MacOsApplicationRuntime {
    fun sharedApplication(): Pointer?
    fun run(application: Pointer)
    fun terminate(application: Pointer)
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
