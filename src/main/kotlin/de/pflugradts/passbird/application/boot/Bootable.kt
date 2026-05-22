package de.pflugradts.passbird.application.boot

import com.google.inject.Guice
import com.google.inject.Module

fun bootModule(module: Module) = Guice.createInjector(module).getInstance(Bootable::class.java).boot()

interface Bootable {
    fun boot()
}
