package de.pflugradts.passbird.domain.model.shell

import de.pflugradts.passbird.domain.model.ddd.ValueObject
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import java.security.SecureRandom
import java.util.stream.Stream
import java.util.stream.StreamSupport

class Shell private constructor(
    private val byteArray: ByteArray,
) : ValueObject,
    Iterable<Byte> {

    val size get() = byteArray.size
    val isEmpty get() = size == 0
    val isNotEmpty get() = !isEmpty
    val firstByte get() = byteArray[0]
    fun getByte(index: Int) = byteArray[index]
    fun getChar(index: Int) = Char(getByte(index).toUShort())
    override fun iterator() = ShellIterator(byteArray.clone())
    fun copy() = shellOf(byteArray.clone())
    fun stream(): Stream<Byte> = StreamSupport.stream(spliterator(), false)
    fun toByteArray() = byteArray.clone()

    fun toPlainShell(): PlainShell {
        val c = CharArray(size)
        for (i in 0 until size) { c[i] = Char(byteArray[i].toUShort()) }
        return plainShellOf(c)
    }

    fun asString(): String {
        val builder = StringBuilder()
        for (i in 0 until size) {
            builder.append(Char(byteArray[i].toUShort()))
        }
        return builder.toString()
    }

    @JvmOverloads
    fun slice(fromInclusive: Int, toExclusive: Int = byteArray.size): Shell =
        if (toExclusive - fromInclusive > 0) {
            val sub = ByteArray(toExclusive - fromInclusive)
            System.arraycopy(byteArray, fromInclusive, sub, 0, sub.size)
            shellOf(sub)
        } else {
            emptyShell()
        }

    fun scramble() = byteArray.indices.forEach {
        byteArray[it] = (SECURE_RANDOM.nextInt(1 + MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE).toByte()
    }

    class ShellIterator(private val byteArray: ByteArray) : Iterator<Byte> {
        private var index = 0
        override fun hasNext() = index < byteArray.size
        override fun next() = if (hasNext()) byteArray[index++] else throw NoSuchElementException()
    }

    override fun equals(other: Any?): Boolean = when {
        (this === other) -> true
        (javaClass != other?.javaClass) -> false
        else -> byteArray contentEquals (other as Shell).byteArray
    }

    override fun hashCode() = byteArray.contentHashCode()

    companion object {
        private val SECURE_RANDOM = SecureRandom()

        @JvmStatic
        fun shellOf(bytes: ByteArray) = Shell(bytes.clone())

        @JvmStatic
        fun shellOf(b: List<Byte>): Shell {
            val bytes = ByteArray(b.size)
            for (i in b.indices) {
                bytes[i] = b[i]
            }
            return shellOf(bytes)
        }

        @JvmStatic
        fun shellOf(s: String): Shell = plainShellOf(s.toCharArray()).toShell()

        @JvmStatic
        fun emptyShell(): Shell = shellOf(ByteArray(0))
    }
}

@JvmInline
value class ShellPair(val value: Pair<Shell, Shell>)
