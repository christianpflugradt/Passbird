package de.pflugradts.passbird.application.passwordtree

import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf

fun checksum(bytes: ByteArray) = bytes.fold(0) { acc, byte -> Integer.sum(acc, byte.toInt()) }.toByte()
fun signature() = byteArrayOf(0x0, 0x50, 0x77, 0x4D, 0x61, 0x6E, 0x36, 0x0)
fun signatureSize() = signature().size
fun intBytes() = Integer.BYTES
fun checksumBytes() = 1
fun placeHolder() = EncryptedShell(payload = shellOf("placeholder"), iv = shellOf("000000000000"))
fun currentPasswordTreePayloadFormatMarker() = Int.MAX_VALUE
fun currentPasswordTreePayloadVersion() = 1
