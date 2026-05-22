package de.pflugradts.kotlinextensions

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success

inline fun <R> tryCatching(block: () -> R) = try {
    success(block())
} catch (ex: Exception) {
    failure(ex)
}

private class Failure(val ex: Exception)

@JvmInline
value class TryResult<R> private constructor(
    val value: Any?,
) {
    val failure: Boolean get() = value is Failure
    val success: Boolean get() = value !is Failure

    fun exceptionOrNull() = when (value) {
        is Failure -> value.ex
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    fun getOrNull() = when (value) {
        is Failure -> null
        else -> value as R
    }

    infix fun getOrElse(other: R) = getOrNull() ?: other

    fun onFailure(action: (ex: Exception) -> Unit) = apply { exceptionOrNull()?.let(action) }
    fun onSuccess(action: (value: R) -> Unit) = apply { getOrNull()?.let(action) }

    fun <T> map(fn: (R) -> T): TryResult<T> = if (success) success(fn(getOrNull()!!)) else failure(exceptionOrNull()!!)

    fun retry(block: (TryResult<R>) -> TryResult<R>) = if (failure) block(this) else this

    companion object {
        fun <R> success(value: R) = TryResult<R>(value)
        fun <R> failure(ex: Exception) = TryResult<R>(Failure(ex))
    }
}
