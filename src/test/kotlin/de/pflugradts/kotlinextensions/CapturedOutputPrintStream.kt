package de.pflugradts.kotlinextensions

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CapturedOutputPrintStream private constructor(
    private val baos: ByteArrayOutputStream,
    private val target: Target,
) : PrintStream(baos) {
    private enum class Target { SYSERR, SYSOUT }
    private var closed = false
    val capture get() = String(baos.toByteArray())
    fun <R> during(block: () -> R): R {
        if (closed) throw IllegalStateException("stream has already been captured")
        when (target) {
            Target.SYSERR -> System.setErr(this)
            Target.SYSOUT -> System.setOut(this)
        }
        val result = block()
        closed = true
        close()
        return result
    }

    companion object {
        fun captureSystemErr() = CapturedOutputPrintStream(ByteArrayOutputStream(), Target.SYSERR)
        fun captureSystemOut() = CapturedOutputPrintStream(ByteArrayOutputStream(), Target.SYSOUT)
        fun <R> mockSystemInWith(string: String, block: () -> R) = ByteArrayInputStream(string.toByteArray()).use {
            System.setIn(it)
            block()
        }
    }
}
