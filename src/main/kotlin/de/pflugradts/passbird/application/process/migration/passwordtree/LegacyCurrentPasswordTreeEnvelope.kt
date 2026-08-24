package de.pflugradts.passbird.application.process.migration.passwordtree

private val LEGACY_CURRENT_PASSWORD_TREE_FILE_HEADER = byteArrayOf(0x0, 0x50, 0x77, 0x54, 0x72, 0x65, 0x65, 0x36, 0x0)

fun wrapLegacyCurrentPasswordTree(bytes: ByteArray) = LEGACY_CURRENT_PASSWORD_TREE_FILE_HEADER + bytes

fun unwrapLegacyCurrentPasswordTree(bytes: ByteArray) = when {
    bytes.isEmpty() -> byteArrayOf()
    isLegacyCurrentPasswordTree(bytes) -> bytes.copyOfRange(LEGACY_CURRENT_PASSWORD_TREE_FILE_HEADER.size, bytes.size)
    else -> throw IllegalStateException("Unsupported password tree format.")
}

fun isLegacyCurrentPasswordTree(bytes: ByteArray) = bytes.size >= LEGACY_CURRENT_PASSWORD_TREE_FILE_HEADER.size &&
    bytes.copyOfRange(0, LEGACY_CURRENT_PASSWORD_TREE_FILE_HEADER.size).contentEquals(LEGACY_CURRENT_PASSWORD_TREE_FILE_HEADER)
