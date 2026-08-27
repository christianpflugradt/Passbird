package de.pflugradts.passbird.application.process.migration.passwordtree

private val LEGACY_TRASH_PASSWORD_TREE_FILE_HEADER = byteArrayOf(0x0, 0x50, 0x77, 0x54, 0x72, 0x65, 0x65, 0x37, 0x0)

fun wrapLegacyTrashPasswordTree(bytes: ByteArray) = LEGACY_TRASH_PASSWORD_TREE_FILE_HEADER + bytes

fun unwrapLegacyTrashPasswordTree(bytes: ByteArray) = when {
    bytes.isEmpty() -> byteArrayOf()
    isLegacyTrashPasswordTree(bytes) -> bytes.copyOfRange(LEGACY_TRASH_PASSWORD_TREE_FILE_HEADER.size, bytes.size)
    else -> throw IllegalStateException("Unsupported password tree format.")
}

fun isLegacyTrashPasswordTree(bytes: ByteArray) = bytes.size >= LEGACY_TRASH_PASSWORD_TREE_FILE_HEADER.size &&
    bytes.copyOfRange(0, LEGACY_TRASH_PASSWORD_TREE_FILE_HEADER.size).contentEquals(LEGACY_TRASH_PASSWORD_TREE_FILE_HEADER)
