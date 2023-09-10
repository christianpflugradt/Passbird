package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.launcher.LauncherModule
import de.pflugradts.passbird.application.configuration.ReadableConfiguration

fun main(args: Array<String>) {
    if (args.size == 1) {
        System.setProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY, args[0])
    }
    bootModule(LauncherModule())
}
