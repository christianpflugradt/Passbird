package de.pflugradts.passbird.adapter.passwordstore

import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import java.util.stream.StreamSupport

fun checksum(bytes: ByteArray) = StreamSupport.stream(shellOf(bytes).spliterator(), false)
    .mapToInt { it.toInt() }
    .reduce(0) { a: Int, b: Int -> Integer.sum(a, b) }.toByte()
fun signature() = byteArrayOf(0x0, 0x50, 0x77, 0x4D, 0x61, 0x6E, 0x34, 0x0)
fun signatureSize() = signature().size
fun intBytes() = Integer.BYTES
fun eofBytes() = intBytes()
fun checksumBytes() = 1
