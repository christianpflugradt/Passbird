package de.pflugradts.kotlinextensions

inline fun <T, R> T.tryCatching(block: T.() -> R): TryResult<R> {
    return try {
        TryResult.success(block())
    } catch (ex: Exception) {
        TryResult.failure(ex)
    }
}

@JvmInline
private value class Failure(val ex: Exception)

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

    fun getOrNull() = when (value) {
        is Failure -> null
        else -> value as R
    }

    infix fun getOrElse(other: R) = when (value) {
        is Failure -> other
        else -> value as R
    }

    fun onFailure(action: (ex: Exception) -> Unit): TryResult<R> {
        exceptionOrNull()?.let { action(it) }
        return this
    }

    fun onSuccess(action: (value: R) -> Unit): TryResult<R> {
        getOrNull()?.let { action(it) }
        return this
    }

    companion object {
        fun <R> success(value: R) = TryResult<R>(value)
        fun <R> failure(ex: Exception) = TryResult<R>(Failure(ex))
    }
}
