package de.pflugradts.pwman3.application

import de.pflugradts.pwman3.application.boot.Bootable
import de.pflugradts.pwman3.application.boot.launcher.LauncherModule
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration
import de.pflugradts.pwman3.application.util.GuiceInjector

class Main(private val guiceInjector: GuiceInjector) {
    fun boot(config: String?) {
        config?.let { System.setProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY, it) }
        guiceInjector.create(LauncherModule()).getInstance(Bootable::class.java).boot()
    }

    companion object {
        private const val EXPECTED_NUMBER_OF_ARGUMENTS = 1

        @JvmStatic
        fun main(args: Array<String>) {
            Main(GuiceInjector()).boot(if (args.size == EXPECTED_NUMBER_OF_ARGUMENTS) args[0] else null)
        }
    }
}
