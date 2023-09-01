package de.pflugradts.passbird.application.failurehandling

import java.io.PrintStream

private val stream: PrintStream = System.err

fun logError(t: Throwable) = t.printStackTrace(stream)
fun logErrorMsg(msg: String) = stream.println(msg)
