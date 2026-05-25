package de.pflugradts.passbird.application.passwordtree

import jakarta.inject.Inject

private val LEGACY_PASSWORD_TREE_FILE_HEADER = byteArrayOf(0x0, 0x50, 0x77, 0x54, 0x72, 0x65, 0x65, 0x35, 0x0)
private val PASSWORD_TREE_FILE_HEADER = byteArrayOf(0x0, 0x50, 0x77, 0x54, 0x72, 0x65, 0x65, 0x36, 0x0)

class PasswordTreeEnvelope @Inject constructor() {
    fun wrap(bytes: ByteArray) = PASSWORD_TREE_FILE_HEADER + bytes
    fun wrapLegacyCurrent(bytes: ByteArray) = LEGACY_PASSWORD_TREE_FILE_HEADER + bytes

    fun unwrap(bytes: ByteArray) = when {
        bytes.isEmpty() -> byteArrayOf()
        isCurrent(bytes) -> bytes.copyOfRange(PASSWORD_TREE_FILE_HEADER.size, bytes.size)
        else -> throw IllegalStateException("Unsupported password tree format.")
    }

    fun unwrapLegacyCurrent(bytes: ByteArray) = when {
        bytes.isEmpty() -> byteArrayOf()
        isLegacyCurrent(bytes) -> bytes.copyOfRange(LEGACY_PASSWORD_TREE_FILE_HEADER.size, bytes.size)
        else -> throw IllegalStateException("Unsupported password tree format.")
    }

    fun isCurrent(bytes: ByteArray) = bytes.size >= PASSWORD_TREE_FILE_HEADER.size &&
        bytes.copyOfRange(0, PASSWORD_TREE_FILE_HEADER.size).contentEquals(PASSWORD_TREE_FILE_HEADER)

    fun isLegacyCurrent(bytes: ByteArray) = bytes.size >= LEGACY_PASSWORD_TREE_FILE_HEADER.size &&
        bytes.copyOfRange(0, LEGACY_PASSWORD_TREE_FILE_HEADER.size).contentEquals(LEGACY_PASSWORD_TREE_FILE_HEADER)
}
