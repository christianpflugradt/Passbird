package de.pflugradts.passbird.application.process

interface Process {
    fun run()
}
interface Initializer : Process
interface Finalizer : Process
