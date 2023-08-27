package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.boot.launcher.LauncherModule
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.GuiceInjector

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
