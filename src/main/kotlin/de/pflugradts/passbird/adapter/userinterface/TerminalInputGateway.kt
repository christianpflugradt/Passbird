package de.pflugradts.passbird.adapter.userinterface

import jakarta.inject.Inject

class TerminalInputGateway @Inject constructor() {
    val isConsoleAvailable: Boolean get() = System.console() != null

    fun readCharFromStdin(): Char = System.`in`.read().toChar()
    fun readPasswordFromConsole(): CharArray = System.console().readPassword()
}
