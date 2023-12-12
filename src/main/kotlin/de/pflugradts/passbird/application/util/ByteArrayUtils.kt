package de.pflugradts.passbird.application.util

import java.nio.ByteBuffer
import java.util.Arrays
import kotlin.math.min

private fun Int.asByteArray() = ByteBuffer.allocate(Integer.BYTES).putInt(this).array()
private fun ByteArray.asInt() = ByteBuffer.wrap(this).getInt()
fun readInt(array: ByteArray, offset: Int) = readBytes(array, offset, Integer.BYTES).asInt()
fun readBytes(array: ByteArray, offset: Int, size: Int): ByteArray = Arrays.copyOfRange(array, offset, size + offset)
fun copyInt(i: Int, target: ByteArray, offset: Int) = copyBytes(i.asByteArray(), target, offset, Integer.BYTES)
fun copyBytes(
    source: ByteArray,
    target: ByteArray,
    offset: Int,
    size: Int = min(source.size, target.size),
): Int {
    System.arraycopy(source, 0, target, offset, size)
    return size
}
