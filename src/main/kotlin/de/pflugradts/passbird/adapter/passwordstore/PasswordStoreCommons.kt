package de.pflugradts.passbird.adapter.passwordstore

import de.pflugradts.passbird.domain.model.transfer.Bytes
import java.util.stream.StreamSupport

internal fun checksum(bytes: ByteArray) = StreamSupport.stream(Bytes.bytesOf(bytes).spliterator(), false)
    .mapToInt { it.toInt() }
    .reduce(0) { a: Int, b: Int -> Integer.sum(a, b) }.toByte()
internal fun signature() = byteArrayOf(0x0, 0x50, 0x77, 0x4D, 0x61, 0x6E, 0x34, 0x0)
internal fun signatureSize() = signature().size
internal fun intBytes() = Integer.BYTES
internal fun eofBytes() = intBytes()
internal fun checksumBytes() = 1
