package de.pflugradts.passbird.application

interface GlobalHotkeyAdapterPort {
    fun prepareOnStartup(): Boolean
    fun register(key: Char): RegisteredGlobalHotkey?
}

interface RegisteredGlobalHotkey {
    fun awaitWithin(milliseconds: Long): Boolean
    fun release()
}
