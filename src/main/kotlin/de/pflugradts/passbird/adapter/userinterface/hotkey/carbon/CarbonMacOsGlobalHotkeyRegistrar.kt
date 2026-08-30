@file:Suppress("ktlint:standard:function-naming")

package de.pflugradts.passbird.adapter.userinterface.hotkey.carbon

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference
import de.pflugradts.passbird.adapter.userinterface.hotkey.PlatformHotkeyRegistrar
import de.pflugradts.passbird.adapter.userinterface.hotkey.macOsKeyCode
import de.pflugradts.passbird.application.RegisteredGlobalHotkey
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal interface CarbonHotkeyRuntime {
    fun open(keyCode: Int, onNextAction: () -> Unit): CarbonMacOsHotkeySession?
}

internal interface CarbonMacOsHotkeySession {
    fun close()
}

internal class CarbonMacOsGlobalHotkeyRegistrar(
    private val keyCodeResolver: (Char) -> Int? = ::macOsKeyCode,
    private val runtimeFactory: () -> CarbonHotkeyRuntime = ::CarbonMacOsHotkeyRuntime,
) : PlatformHotkeyRegistrar {
    override fun register(key: Char): RegisteredGlobalHotkey? = runCatching {
        keyCodeResolver(key)
            ?.let { CarbonMacOsRegistration.open(it, runtimeFactory()) }
    }.getOrNull()

    internal class CarbonMacOsRegistration(
        private val session: CarbonMacOsHotkeySession,
        private val nextActions: Semaphore,
    ) : RegisteredGlobalHotkey {
        private val released = AtomicBoolean(false)

        override fun awaitWithin(milliseconds: Long) = nextActions.tryAcquire(milliseconds, TimeUnit.MILLISECONDS)

        override fun release() {
            if (released.compareAndSet(false, true)) {
                session.close()
            }
        }

        companion object {
            fun open(keyCode: Int, runtime: CarbonHotkeyRuntime): CarbonMacOsRegistration? {
                val nextActions = Semaphore(0)
                val session = runtime.open(keyCode) {
                    nextActions.release()
                } ?: return null
                return CarbonMacOsRegistration(session, nextActions)
            }
        }
    }
}

internal class CarbonMacOsHotkeyRuntime(
    private val carbon: Carbon = Carbon.instance(),
    private val mainThreadExecutor: CarbonMacOsMainThreadExecutor = CarbonMacOsRuntimeContextExecutor,
) : CarbonHotkeyRuntime {
    override fun open(keyCode: Int, onNextAction: () -> Unit): CarbonMacOsHotkeySession? = mainThreadExecutor.dispatch {
        val eventTarget = carbon.GetApplicationEventTarget() ?: return@dispatch null
        val eventType = CarbonEventTypeSpec().apply {
            eventClass = CARBON_EVENT_CLASS_KEYBOARD
            eventKind = CARBON_EVENT_HOTKEY_PRESSED
            write()
        }
        val eventHandler = Carbon.EventHandler { _, _, _ ->
            onNextAction()
            CARBON_SUCCESS
        }
        val eventHandlerReference = PointerByReference()
        if (carbon.InstallEventHandler(eventTarget, eventHandler, 1, eventType, Pointer.NULL, eventHandlerReference) != CARBON_SUCCESS) {
            return@dispatch null
        }
        val hotkeyReference = PointerByReference()
        val hotkeyId = CarbonEventHotkeyId.ByValue().apply {
            signature = CARBON_HOTKEY_SIGNATURE
            id = CARBON_HOTKEY_ID
            write()
        }
        if (
            carbon.RegisterEventHotKey(
                keyCode,
                CARBON_CONTROL_KEY or CARBON_SHIFT_KEY,
                hotkeyId,
                eventTarget,
                0,
                hotkeyReference,
            ) != CARBON_SUCCESS
        ) {
            eventHandlerReference.value?.let(carbon::RemoveEventHandler)
            return@dispatch null
        }
        CarbonMacOsHotkeyLoop(carbon, hotkeyReference.value, eventHandlerReference.value, eventHandler, mainThreadExecutor)
    }
}

internal object CarbonMacOsRuntimeContextExecutor : CarbonMacOsMainThreadExecutor {
    override fun <T> dispatch(work: () -> T): T = CarbonMacOsRuntimeContext.dispatch(work)
        ?: throw IllegalStateException("Carbon macOS main thread runtime is not active")
}

internal class CarbonMacOsHotkeyLoop(
    private val carbon: Carbon,
    private val hotkeyReference: Pointer?,
    private val eventHandlerReference: Pointer?,
    @Suppress("UNUSED_PARAMETER") private val eventHandler: Carbon.EventHandler,
    private val mainThreadExecutor: CarbonMacOsMainThreadExecutor,
) : CarbonMacOsHotkeySession {
    private val closed = AtomicBoolean(false)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching {
                mainThreadExecutor.dispatch {
                    hotkeyReference?.let(carbon::UnregisterEventHotKey)
                    eventHandlerReference?.let(carbon::RemoveEventHandler)
                }
            }
        }
    }
}

internal interface Carbon : Library {
    fun GetApplicationEventTarget(): Pointer?
    fun InstallEventHandler(
        target: Pointer,
        handler: EventHandler,
        numTypes: Int,
        eventTypes: CarbonEventTypeSpec,
        userData: Pointer?,
        eventHandlerRef: PointerByReference,
    ): Int

    fun RegisterEventHotKey(
        keyCode: Int,
        modifiers: Int,
        hotkeyId: CarbonEventHotkeyId.ByValue,
        target: Pointer,
        options: Int,
        hotkeyRef: PointerByReference,
    ): Int

    fun UnregisterEventHotKey(hotkeyRef: Pointer): Int
    fun RemoveEventHandler(eventHandlerRef: Pointer): Int

    fun interface EventHandler : Callback {
        fun callback(nextHandler: Pointer?, event: Pointer?, userData: Pointer?): Int
    }

    companion object {
        fun instance(): Carbon = Native.load(
            "/System/Library/Frameworks/Carbon.framework/Frameworks/HIToolbox.framework/HIToolbox",
            Carbon::class.java,
        )
    }
}

@Structure.FieldOrder("eventClass", "eventKind")
internal class CarbonEventTypeSpec : Structure() {
    @JvmField
    var eventClass = 0

    @JvmField
    var eventKind = 0
}

@Structure.FieldOrder("signature", "id")
internal open class CarbonEventHotkeyId : Structure() {
    @JvmField
    var signature = 0

    @JvmField
    var id = 0

    class ByValue : CarbonEventHotkeyId(), Structure.ByValue
}

private fun fourCC(value: String) = (value[0].code shl 24) or
    (value[1].code shl 16) or
    (value[2].code shl 8) or
    value[3].code

private const val CARBON_CONTROL_KEY = 1 shl 12
private const val CARBON_SHIFT_KEY = 1 shl 9
private const val CARBON_EVENT_HOTKEY_PRESSED = 6
private const val CARBON_SUCCESS = 0
private const val CARBON_HOTKEY_ID = 1
private val CARBON_EVENT_CLASS_KEYBOARD = fourCC("keyb")
private val CARBON_HOTKEY_SIGNATURE = fourCC("PSBD")
