package de.pflugradts.kotlinextensions

import com.google.inject.Provider

class GuiceTestProvider<T>(private val t: T) : Provider<T> {
    override fun get() = t
}
