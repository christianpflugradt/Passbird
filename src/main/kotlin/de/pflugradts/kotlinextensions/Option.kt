package de.pflugradts.kotlinextensions

import de.pflugradts.kotlinextensions.MutableOption.Companion.optionOf
import java.util.Optional

interface Option<T> {
    val isPresent: Boolean
    val isEmpty: Boolean
    fun get(): T
    fun orNull(): T?
    fun orElse(other: T): T
    fun ifPresent(block: (T) -> Unit)
    fun ifPresentOrElse(block: (T) -> Unit, other: () -> Unit)
    fun <U> map(block: (T) -> U): Option<U>
    fun or(block: () -> Option<T>): Option<T>
}

class MutableOption<T> private constructor(private var value: T?) : Option<T> {
    override val isPresent get() = value != null
    override val isEmpty get() = !isPresent
    override fun get() = value!!
    override fun orNull() = value
    override fun orElse(other: T) = value ?: other
    override fun ifPresent(block: (T) -> Unit) {
        value?.let { block(it) }
    }
    override fun ifPresentOrElse(block: (T) -> Unit, other: () -> Unit) {
        ifPresent(block)
        value ?: other()
    }
    override fun <U> map(block: (T) -> U) = value?.let { optionOf(block(it)) } ?: emptyOption()
    override fun or(block: () -> Option<T>) = if (isPresent) this else block()
    fun set(value: T?) {
        this.value = value
    }
    companion object {
        fun <T> emptyOption(): Option<T> = MutableOption(null)
        fun <T> optionOf(value: T? = null): Option<T> = MutableOption(value)
        fun <T> mutableOptionOf(value: T? = null): MutableOption<T> = MutableOption(value)
    }
}

fun <T> Optional<T>.toOption() = optionOf(this.orElse(null))
