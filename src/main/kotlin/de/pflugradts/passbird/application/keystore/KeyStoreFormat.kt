package de.pflugradts.passbird.application.keystore

enum class KeyStoreFormat {
    JCEKS,
    PKCS12,
    UNKNOWN,
}

class KeyStoreFormatDetector {
    fun detect(bytes: ByteArray) = when {
        bytes.startsWith(JCEKS_MAGIC) -> KeyStoreFormat.JCEKS
        bytes.startsWith(PKCS12_SEQUENCE_HEADER) -> KeyStoreFormat.PKCS12
        else -> KeyStoreFormat.UNKNOWN
    }
}

private val JCEKS_MAGIC = byteArrayOf(
    0xCE.toByte(),
    0xCE.toByte(),
    0xCE.toByte(),
    0xCE.toByte(),
)
private val PKCS12_SEQUENCE_HEADER = byteArrayOf(0x30)

private fun ByteArray.startsWith(prefix: ByteArray) = size >= prefix.size && prefix.indices.all { index -> this[index] == prefix[index] }
