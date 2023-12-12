package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes

class Output private constructor(val bytes: Bytes) {

    override fun equals(other: Any?): Boolean = when {
        (this === other) -> true
        (javaClass != other?.javaClass) -> false
        else -> bytes == (other as Output).bytes
    }
    override fun hashCode() = bytes.hashCode()

    companion object {
        fun outputOf(bytes: Bytes) = Output(bytes)
        fun outputOf(byteArray: ByteArray) = outputOf(Bytes.bytesOf(byteArray))
        fun emptyOutput() = outputOf(emptyBytes())
    }
}
