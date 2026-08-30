package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.boot.launcher.LauncherGraph
import de.pflugradts.passbird.application.boot.main.MacOsApplicationLoopGraph
import de.pflugradts.passbird.application.failure.HomeDirectoryFailure
import de.pflugradts.passbird.application.failure.HomeDirectoryFailureCase.DOES_NOT_EXIST
import de.pflugradts.passbird.application.failure.HomeDirectoryFailureCase.IS_NOT_A_DIRECTORY
import de.pflugradts.passbird.application.failure.HomeDirectoryFailureCase.IS_NULL
import de.pflugradts.passbird.application.failure.InitialNestSlotFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT

data class MainLauncherRuntime(
    val globalHotkeyEnabled: Boolean,
    val globalHotkeyBackend: GlobalHotkeyBackend,
    val boot: () -> Unit,
)

fun mainGetSystemOperation() = SystemOperation()

fun mainHasValidHomeDirectory(dir: String?): Boolean {
    var valid = false
    when {
        dir == null -> reportFailure(HomeDirectoryFailure(case = IS_NULL))
        !mainGetSystemOperation().exists(dir.toDirectory()) -> reportFailure(HomeDirectoryFailure(dir, DOES_NOT_EXIST))
        !mainGetSystemOperation().isDirectory(dir.toDirectory()) -> reportFailure(HomeDirectoryFailure(dir, IS_NOT_A_DIRECTORY))
        else -> valid = true
    }
    return valid
}

fun mainInitialSlot(slot: String?): Slot? = when (slot) {
    null -> DEFAULT

    else -> {
        val initialSlot = slotAt(slot)
        if (initialSlot != DEFAULT) {
            initialSlot
        } else {
            reportFailure(InitialNestSlotFailure(slot))
            null
        }
    }
}

fun mainLauncherRuntime(runContext: RunContext): MainLauncherRuntime {
    val launcherGraph = LauncherGraph(runContext)
    return MainLauncherRuntime(
        globalHotkeyEnabled = launcherGraph.configuration.application.flow.globalHotkey.enabled,
        globalHotkeyBackend = launcherGraph.configuration.application.flow.globalHotkey.backend,
        boot = launcherGraph.bootable::boot,
    )
}

fun mainBootLauncher(runContext: RunContext) {
    mainBootLauncher(mainLauncherRuntime(runContext))
}

fun mainBootLauncher(
    launcherRuntime: MainLauncherRuntime,
    macOsApplicationLoopRunner: (Boolean, GlobalHotkeyBackend, () -> Unit) -> Unit =
        { globalHotkeyEnabled, globalHotkeyBackend, boot ->
            MacOsApplicationLoopGraph(
                globalHotkeyEnabled = globalHotkeyEnabled,
                globalHotkeyBackend = globalHotkeyBackend,
            ).run(boot)
        },
) {
    macOsApplicationLoopRunner(
        launcherRuntime.globalHotkeyEnabled,
        launcherRuntime.globalHotkeyBackend,
        launcherRuntime.boot,
    )
}

fun main(args: Array<String>) {
    mainRun(args)
}

fun mainRun(args: Array<String>) {
    if (mainHasValidHomeDirectory(args.getOrNull(0))) {
        val initialSlot = mainInitialSlot(args.getOrNull(1))
        if (initialSlot != null) {
            mainBootLauncher(
                PassbirdRunContext(
                    homeDirectory = args[0].toDirectory(),
                    initialSlot = initialSlot,
                ),
            )
        } else {
            mainGetSystemOperation().exit()
        }
    } else {
        mainGetSystemOperation().exit()
    }
}
