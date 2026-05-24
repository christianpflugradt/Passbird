package de.pflugradts.passbird.property

import de.pflugradts.kotlinextensions.TryResult

fun <T> TryResult<T>.orThrow(operation: String): T = getOrNull() ?: throw AssertionError(
    "$operation failed",
    exceptionOrNull(),
)
