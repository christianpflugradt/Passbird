package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.launcher.LauncherModule
import de.pflugradts.passbird.application.failure.HomeDirectoryFailure
import de.pflugradts.passbird.application.failure.HomeDirectoryFailureCase.DOES_NOT_EXIST
import de.pflugradts.passbird.application.failure.HomeDirectoryFailureCase.IS_NOT_A_DIRECTORY
import de.pflugradts.passbird.application.failure.HomeDirectoryFailureCase.IS_NULL
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.SystemOperation
import java.nio.file.Files
import java.nio.file.Paths

interface RunContext {
    val homeDirectory: Directory
}

private lateinit var global: RunContext
val Global get() = global

fun mainGetSystemOperation() = SystemOperation()

fun mainCheckHomeDirectory(dir: String?) {
    when {
        dir == null -> reportFailure(HomeDirectoryFailure(case = IS_NULL))
        !Files.exists(Paths.get(dir)) -> reportFailure(HomeDirectoryFailure(dir, DOES_NOT_EXIST))
        !Files.isDirectory(Paths.get(dir)) -> reportFailure(HomeDirectoryFailure(dir, IS_NOT_A_DIRECTORY))
        else -> return
    }
    mainGetSystemOperation().exit()
}

fun main(args: Array<String>) {
    mainCheckHomeDirectory(args.getOrNull(0))
    global = object : RunContext {
        override val homeDirectory = args[0].toDirectory()
    }
    bootModule(LauncherModule())
}
