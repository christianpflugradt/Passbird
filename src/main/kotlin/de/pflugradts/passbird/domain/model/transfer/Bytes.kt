package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.ddd.ValueObject
import de.pflugradts.passbird.domain.model.transfer.Chars.Companion.charsOf
import java.security.SecureRandom
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 *
 * A Bytes instance is the basic structure to represent information.
 *
 * Any content, be it public or sensitive data, should always be represented as Bytes.
 * A Bytes can be constructed from and converted to other data structures such as String, byte[] or
 * [Chars]. Sensitive data should never be converted to String unless it's inevitable.
 *
 * A [CryptoProvider] can encrypt/decrypt a Bytes.
 * On an unencrypted Bytes [.scramble] should always be called after using it.
 */
class Bytes private constructor(
    private val byteArray: ByteArray,
) : ValueObject,
    Iterable<Byte> {

    val size get() = byteArray.size
    val isEmpty get() = size == 0
    val firstByte get() = byteArray[0]
    fun getByte(index: Int) = byteArray[index]
    fun getChar(index: Int) = Char(getByte(index).toUShort())
    override fun iterator() = BytesIterator(byteArray.clone())
    fun copy() = bytesOf(byteArray.clone())
    fun stream(): Stream<Byte> = StreamSupport.stream(spliterator(), false)
    fun toByteArray() = byteArray.clone()

    fun toChars(): Chars {
        val c = CharArray(size)
        for (i in 0 until size) { c[i] = Char(byteArray[i].toUShort()) }
        return charsOf(c)
    }

    fun asString(): String {
        val builder = StringBuilder()
        for (i in 0 until size) {
            builder.append(Char(byteArray[i].toUShort()))
        }
        return builder.toString()
    }

    @JvmOverloads
    fun slice(fromInclusive: Int, toExclusive: Int = byteArray.size) =
        if (toExclusive - fromInclusive > 0) {
            val sub = ByteArray(toExclusive - fromInclusive)
            System.arraycopy(byteArray, fromInclusive, sub, 0, sub.size)
            bytesOf(sub)
        } else {
            emptyBytes()
        }

    fun scramble() = byteArray.indices.forEach {
        byteArray[it] = (SECURE_RANDOM.nextInt(1 + MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE).toByte()
    }

    class BytesIterator(private val byteArray: ByteArray) : Iterator<Byte> {
        private var index = 0
        override fun hasNext() = index < byteArray.size
        override fun next() = if (hasNext()) byteArray[index++] else throw NoSuchElementException()
    }

    override fun equals(other: Any?): Boolean = when {
        (this === other) -> true
        (javaClass != other?.javaClass) -> false
        else -> byteArray contentEquals (other as Bytes).byteArray
    }

    override fun hashCode() = byteArray.contentHashCode()

    companion object {
        private val SECURE_RANDOM = SecureRandom()

        @JvmStatic
        fun bytesOf(bytes: ByteArray) = Bytes(bytes.clone())

        @JvmStatic
        fun bytesOf(b: List<Byte>): Bytes {
            val bytes = ByteArray(b.size)
            for (i in b.indices) {
                bytes[i] = b[i]
            }
            return bytesOf(bytes)
        }

        @JvmStatic
        fun bytesOf(s: String): Bytes = charsOf(s.toCharArray()).toBytes()

        @JvmStatic
        fun emptyBytes(): Bytes = bytesOf(ByteArray(0))
    }
}
