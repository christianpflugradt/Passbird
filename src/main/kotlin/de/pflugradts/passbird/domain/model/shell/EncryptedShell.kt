package de.pflugradts.passbird.domain.model.shell

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf

class EncryptedShell(val payload: Shell, val iv: Shell) {

    val size get() = iv.size + payload.size

    fun copy() = EncryptedShell(payload.copy(), iv.copy())

    fun scramble() = apply {
        payload.scramble()
        iv.scramble()
    }

    fun toByteArray() = iv.toByteArray() + payload.toByteArray()

    override fun equals(other: Any?) = (other as? EncryptedShell)?.let {
        it.payload == payload && it.iv == iv
    } ?: false
    override fun hashCode() = payload.hashCode() + 31 * iv.hashCode()

    companion object {
        fun encryptedShellOf(byteArray: ByteArray) = EncryptedShell(
            iv = shellOf(byteArray.copyOfRange(0, 12)),
            payload = shellOf(byteArray.copyOfRange(12, byteArray.size)),
        )
    }
}
