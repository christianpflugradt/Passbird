package de.pflugradts.passbird.adapter.passwordstore

import com.google.inject.Inject
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.application.util.copyInt
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.NamespaceService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import java.util.Arrays
import java.util.function.Supplier
import java.util.stream.Stream

private const val EOF = 0
private const val SECTOR = -1
private const val EMPTY_NAMESPACE = -2

internal class PasswordStoreWriter @Inject constructor(
    @Inject private val systemOperation: SystemOperation,
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val namespaceService: NamespaceService,
    @Inject private val cryptoProvider: CryptoProvider,
) {

    fun sync(passwordEntriesSupplier: Supplier<Stream<PasswordEntry>>) {
        val contentSize = calcRequiredContentSize(passwordEntriesSupplier)
        val bytes = ByteArray(calcActualTotalSize(contentSize))
        var offset = copyBytes(signature(), bytes, 0, signatureSize())
        offset = persistNamespaces(bytes, offset)
        passwordEntriesSupplier.get().forEach { passwordEntry ->
            passwordEntry.asByteArray().let { offset += copyBytes(it, bytes, offset, it.size) }
        }
        offset += copyInt(EOF, bytes, offset)
        val checksumBytes = byteArrayOf(if (contentSize > 0) checksum(Arrays.copyOfRange(bytes, signatureSize(), contentSize)) else 0x0)
        copyBytes(checksumBytes, bytes, offset, checksumBytes())
        writeToDisk(bytesOf(bytes))
    }

    private fun persistNamespaces(bytes: ByteArray, offset: Int): Int {
        var incrementedOffset = offset
        copyInt(SECTOR, bytes, incrementedOffset)
        incrementedOffset += intBytes()
        for (index in NamespaceSlot.FIRST_NAMESPACE..NamespaceSlot.LAST_NAMESPACE) {
            NamespaceSlot.at(index).asByteArray().let { incrementedOffset += copyBytes(it, bytes, incrementedOffset, it.size) }
        }
        return incrementedOffset
    }

    private fun writeToDisk(bytes: Bytes) = systemOperation.writeBytesToFile(filePath!!, cryptoProvider.encrypt(bytes))

    private fun calcRequiredContentSize(passwordEntries: Supplier<Stream<PasswordEntry>>): Int {
        val dataSize = passwordEntries.get()
            .map { passwordEntry: PasswordEntry -> intBytes() + passwordEntry.viewKey().size + passwordEntry.viewPassword().size }
            .reduce(0) { a: Int, b: Int -> Integer.sum(a, b) }
        val namespaceSize = intBytes() + NamespaceSlot.CAPACITY * intBytes() + namespaceService.all()
            .filter { it.isPresent }
            .map { it.get().bytes.size }
            .reduce(0) { a: Int, b: Int -> Integer.sum(a, b) }
        val metaSize = passwordEntries.get().count().toInt() * 2 * intBytes()
        return dataSize + namespaceSize + metaSize
    }

    private val filePath get() = systemOperation.resolvePath(
        configuration.getAdapter().getPasswordStore().getLocation(),
        ReadableConfiguration.DATABASE_FILENAME,
    )

    private fun calcActualTotalSize(contentSize: Int) = signatureSize() + contentSize + eofBytes() + checksumBytes()

    private fun NamespaceSlot.asByteArray(): ByteArray {
        val namespaceBytes = namespaceService.atSlot(this).map { it.bytes }.orElse(Bytes.emptyBytes())
        val namespaceBytesSize = namespaceBytes.size
        val bytes = ByteArray(Integer.BYTES + namespaceBytesSize)
        copyInt(if (namespaceBytes.isEmpty) EMPTY_NAMESPACE else namespaceBytesSize, bytes, 0)
        if (!namespaceBytes.isEmpty) { copyBytes(namespaceBytes.toByteArray(), bytes, Integer.BYTES, namespaceBytesSize) }
        return bytes
    }

    private fun PasswordEntry.asByteArray(): ByteArray {
        val keySize = viewKey().size
        val passwordSize = viewPassword().size
        val metaSize = 2 * Integer.BYTES
        val bytes = ByteArray(Integer.BYTES + keySize + passwordSize + metaSize)
        var offset = copyInt(associatedNamespace().index(), bytes, 0)
        offset += copyInt(keySize, bytes, offset)
        offset += copyBytes(viewKey().toByteArray(), bytes, offset, keySize)
        offset += copyInt(passwordSize, bytes, offset)
        copyBytes(viewPassword().toByteArray(), bytes, offset, passwordSize)
        return bytes
    }
}
