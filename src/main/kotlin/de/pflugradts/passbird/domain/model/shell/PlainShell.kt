package de.pflugradts.passbird.domain.model.shell

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import java.security.SecureRandom

class PlainShell private constructor(private val content: CharArray) {

    fun scramble() = content.indices.forEach {
        content[it] = (SECURE_RANDOM.nextInt(1 + MAX_ASCII_VALUE - MIN_ASCII_VALUE) + MIN_ASCII_VALUE).toChar()
    }

    fun toShell(): Shell = shellOf(
        ByteArray(content.size).also { byteArray ->
            content.indices.forEach { index -> byteArray[index] = content[index].code.toByte() }
        },
    ).also { this.scramble() }

    fun toCharArray() = content.clone()

    override fun equals(other: Any?): Boolean = when {
        (this === other) -> true
        (javaClass != other?.javaClass) -> false
        else -> content contentEquals (other as PlainShell).content
    }

    override fun hashCode() = content.contentHashCode()

    companion object {
        val SECURE_RANDOM = SecureRandom()

        @JvmStatic
        fun plainShellOf(chars: CharArray) = PlainShell(chars.clone())
    }
}
