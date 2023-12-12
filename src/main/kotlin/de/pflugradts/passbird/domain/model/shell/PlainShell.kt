package de.pflugradts.passbird.domain.model.shell

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import java.security.SecureRandom

class PlainShell private constructor(private val charArray: CharArray) {

    fun scramble() = charArray.indices.forEach {
        charArray[it] = (SECURE_RANDOM.nextInt(1 + MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE).toChar()
    }

    fun toShell(): Shell = shellOf(
        ByteArray(charArray.size).also { byteArray ->
            charArray.indices.forEach { index -> byteArray[index] = charArray[index].code.toByte() }
        },
    ).also { this.scramble() }

    fun toCharArray() = charArray.clone()

    override fun equals(other: Any?): Boolean = when {
        (this === other) -> true
        (javaClass != other?.javaClass) -> false
        else -> charArray contentEquals (other as PlainShell).charArray
    }

    override fun hashCode() = charArray.contentHashCode()

    companion object {
        val SECURE_RANDOM = SecureRandom()

        @JvmStatic
        fun plainShellOf(chars: CharArray) = PlainShell(chars.clone())
    }
}
