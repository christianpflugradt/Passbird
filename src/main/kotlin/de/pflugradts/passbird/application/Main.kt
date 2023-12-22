package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.launcher.LauncherModule

interface RunContext {
    val homeDirectory: Directory
}

private lateinit var global: RunContext
val Global get() = global

fun main(args: Array<String>) {
    global = object : RunContext {
        override val homeDirectory = if (args.size == 1) args[0].toDirectory() else "".toDirectory()
    }
    bootModule(LauncherModule())
}
