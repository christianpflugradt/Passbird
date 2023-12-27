package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.launcher.LauncherModule
import de.pflugradts.passbird.application.failure.HomeDirectoryFailure
import de.pflugradts.passbird.application.failure.HomeDirectoryFailureCase.DOES_NOT_EXIST
import de.pflugradts.passbird.application.failure.HomeDirectoryFailureCase.IS_NOT_A_DIRECTORY
import de.pflugradts.passbird.application.failure.HomeDirectoryFailureCase.IS_NULL
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.at

interface RunContext {
    val homeDirectory: Directory
    val initialNestSlot: NestSlot
}

private lateinit var global: RunContext
val Global get() = global

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

fun main(args: Array<String>) {
    if (mainHasValidHomeDirectory(args.getOrNull(0))) {
        global = object : RunContext {
            override val homeDirectory = args[0].toDirectory()
            override val initialNestSlot = at(args.getOrElse(1) { 0.toString() })
        }
        bootModule(LauncherModule())
    } else {
        mainGetSystemOperation().exit()
    }
}
