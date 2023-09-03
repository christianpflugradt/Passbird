package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.transfer.CharValue.MAX_ASCII_VALUE
import de.pflugradts.passbird.domain.model.transfer.CharValue.MIN_ASCII_VALUE
import java.security.SecureRandom

class Chars private constructor(private val chars: CharArray) {

    fun scramble() = chars.indices.forEach {
        chars[it] = (SECURE_RANDOM.nextInt(1 + MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE).toChar()
    }

    fun toBytes(): Bytes = Bytes.of(
        *ByteArray(chars.size).also { byteArray ->
            chars.indices.forEach { index -> byteArray[index] = chars[index].code.toByte() }
        },
    ).also { this.scramble() }

    fun toCharArray() = chars.clone()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return chars contentEquals (other as Chars).chars
    }
    /* TODO why does this always return true?
    override fun equals(other: Any?): Boolean = when (other) {
        (this === other) -> true
        (javaClass != other?.javaClass) -> false
        else -> chars contentEquals (other as Chars).chars
    }*/

    override fun hashCode() = chars.contentHashCode()

    companion object {
        val SECURE_RANDOM = SecureRandom()

        @JvmStatic
        fun of(chars: CharArray) = Chars(chars.clone())
    }
}
