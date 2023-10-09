package de.pflugradts.passbird.adapter.passwordstore

import com.google.inject.Inject
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.failurehandling.FailureCollector
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.application.util.readBytes
import de.pflugradts.passbird.application.util.readInt
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.NamespaceService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import java.util.ArrayDeque
import java.util.Arrays
import java.util.function.Supplier
import java.util.stream.Stream

private const val EOF = 0
private const val SECTOR = -1

class PasswordStoreReader @Inject constructor(
    @Inject private val systemOperation: SystemOperation,
    @Inject private val failureCollector: FailureCollector,
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val namespaceService: NamespaceService,
    @Inject private val cryptoProvider: CryptoProvider,
) {
    fun restore(): Supplier<Stream<PasswordEntry>> {
        val passwordEntries = ArrayDeque<PasswordEntry>()
        val bytes = readFromDisk()
        val byteArray = bytes.toByteArray()
        if (byteArray.isNotEmpty()) {
            verifySignature(byteArray)
            verifyChecksum(byteArray)
            var offset = signatureSize()
            val res1 = populateNamespaces(byteArray, offset)
            offset = res1.first
            while (EOF != readInt(byteArray, offset)) {
                val res2 = byteArray.asPasswordEntry(offset, res1.second)
                passwordEntries.add(res2.first)
                offset = res2.second
            }
            return Supplier { passwordEntries.stream() }
        }
        namespaceService.populateEmpty()
        return Supplier { Stream.empty() }
    }

    private fun readFromDisk() = systemOperation.readBytesFromFile(filePath!!).let { if (it.isEmpty) it else cryptoProvider.decrypt(it) }

    private fun verifySignature(bytes: ByteArray) {
        val expectedSignature = signature()
        val actualSignature = ByteArray(signatureSize())
        copyBytes(bytesOf(bytes).toByteArray(), actualSignature, 0, signatureSize())
        if (!expectedSignature.contentEquals(actualSignature)) { failureCollector.collectSignatureCheckFailure(bytesOf(actualSignature)) }
    }

    private fun verifyChecksum(bytes: ByteArray) {
        val contentSize = calcActualContentSize(bytes.size)
        val expectedChecksum = if (contentSize > 0) checksum(Arrays.copyOfRange(bytes, signatureSize(), contentSize)) else 0x0
        val actualCheckSum = bytes[bytes.size - 1]
        if (expectedChecksum != actualCheckSum) { failureCollector.collectChecksumFailure(actualCheckSum, expectedChecksum) }
    }

    private fun populateNamespaces(bytes: ByteArray, offset: Int): Pair<Int, Boolean> {
        var incrementedOffset = offset
        var legacyMode = true
        if (SECTOR == readInt(bytes, incrementedOffset)) {
            incrementedOffset += intBytes()
            val namespaceBytes: MutableList<Bytes> = ArrayList()
            for (i in 0 until NamespaceSlot.CAPACITY) {
                bytes.asNamespaceBytes(incrementedOffset).let {
                    namespaceBytes.add(it.first)
                    incrementedOffset += it.second
                }
            }
            namespaceService.populate(namespaceBytes)
            legacyMode = false
        }
        return Pair(incrementedOffset, legacyMode)
    }

    private val filePath get() = systemOperation.resolvePath(
        configuration.getAdapter().getPasswordStore().getLocation(),
        ReadableConfiguration.DATABASE_FILENAME,
    )

    private fun calcActualContentSize(totalSize: Int) = totalSize - signatureSize() - checksumBytes() - eofBytes()

    private fun ByteArray.asNamespaceBytes(offset: Int): Pair<Bytes, Int> {
        var incrementedOffset = offset
        val namespaceSize = readInt(this, incrementedOffset)
        incrementedOffset += Integer.BYTES
        val result: Bytes
        if (namespaceSize > 0) {
            val namespaceBytes = readBytes(this, incrementedOffset, namespaceSize)
            incrementedOffset += namespaceBytes.size
            result = bytesOf(namespaceBytes)
        } else {
            result = Bytes.emptyBytes()
        }
        return Pair(result, incrementedOffset - offset)
    }

    private fun ByteArray.asPasswordEntry(offset: Int, legacyMode: Boolean): Pair<PasswordEntry, Int> {
        var incrementedOffset = offset
        val namespaceSlot = if (legacyMode) NamespaceSlot.DEFAULT.index() else readInt(this, incrementedOffset)
        incrementedOffset += if (legacyMode) 0 else Integer.BYTES
        val keySize = readInt(this, incrementedOffset)
        incrementedOffset += Integer.BYTES
        val keyBytes = readBytes(this, incrementedOffset, keySize)
        incrementedOffset += keySize
        val passwordSize = readInt(this, incrementedOffset)
        incrementedOffset += Integer.BYTES
        val passwordBytes = readBytes(this, incrementedOffset, passwordSize)
        incrementedOffset += passwordSize
        return Pair(
            PasswordEntry.create(NamespaceSlot.at(namespaceSlot), bytesOf(keyBytes), bytesOf(passwordBytes)),
            incrementedOffset,
        )
    }
}
