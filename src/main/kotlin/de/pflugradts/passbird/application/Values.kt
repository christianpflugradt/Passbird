package de.pflugradts.passbird.application

@JvmInline
value class Directory(val value: String)
fun String.toDirectory() = Directory(this)

@JvmInline
value class FileName(val value: String)
fun String.toFileName() = FileName(this)
