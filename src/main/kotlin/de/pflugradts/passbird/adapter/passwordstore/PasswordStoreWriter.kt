package de.pflugradts.passbird.adapter.passwordstore

import com.google.inject.Inject
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.DATABASE_FILENAME
import de.pflugradts.passbird.application.failure.WritePasswordDatabaseFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.application.util.copyInt
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import java.util.Arrays
import java.util.function.Supplier
import java.util.stream.Stream

private const val EOF = 0
private const val SECTOR = -1
private const val EMPTY_NEST = -2

class PasswordStoreWriter @Inject constructor(
    @Inject private val systemOperation: SystemOperation,
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val nestService: NestService,
    @Inject private val cryptoProvider: CryptoProvider,
) {

    fun sync(passwordEntriesSupplier: Supplier<Stream<PasswordEntry>>) {
        val contentSize = calcRequiredContentSize(passwordEntriesSupplier)
        val bytes = ByteArray(calcActualTotalSize(contentSize))
        var offset = copyBytes(signature(), bytes, 0, signatureSize())
        offset = persistNest(bytes, offset)
        passwordEntriesSupplier.get().forEach { passwordEntry ->
            passwordEntry.asByteArray().let { offset += copyBytes(it, bytes, offset, it.size) }
        }
        offset += copyInt(EOF, bytes, offset)
        val checksumBytes = byteArrayOf(if (contentSize > 0) checksum(Arrays.copyOfRange(bytes, signatureSize(), contentSize)) else 0x0)
        copyBytes(checksumBytes, bytes, offset, checksumBytes())
        writeToDisk(bytesOf(bytes))
    }

    private fun persistNest(bytes: ByteArray, offset: Int): Int {
        var incrementedOffset = offset
        copyInt(SECTOR, bytes, incrementedOffset)
        incrementedOffset += intBytes()
        for (index in Slot.FIRST_SLOT..Slot.LAST_SLOT) {
            Slot.at(index).asByteArray().let { incrementedOffset += copyBytes(it, bytes, incrementedOffset, it.size) }
        }
        return incrementedOffset
    }

    private fun writeToDisk(bytes: Bytes) =
        tryCatching { systemOperation.writeBytesToFile(filePath!!, cryptoProvider.encrypt(bytes)) }
            .onFailure { reportFailure(WritePasswordDatabaseFailure(filePath!!, it)) }

    private fun calcRequiredContentSize(passwordEntries: Supplier<Stream<PasswordEntry>>): Int {
        val dataSize = passwordEntries.get()
            .map { passwordEntry: PasswordEntry -> intBytes() + passwordEntry.viewKey().size + passwordEntry.viewPassword().size }
            .reduce(0) { a: Int, b: Int -> Integer.sum(a, b) }
        val nestSize = intBytes() + Slot.CAPACITY * intBytes() + nestService.all()
            .filter { it.isPresent }
            .map { it.get().bytes.size }
            .reduce(0) { a: Int, b: Int -> Integer.sum(a, b) }
        val metaSize = passwordEntries.get().count().toInt() * 2 * intBytes()
        return dataSize + nestSize + metaSize
    }

    private val filePath get() = systemOperation.resolvePath(configuration.adapter.passwordStore.location, DATABASE_FILENAME)
    private fun calcActualTotalSize(contentSize: Int) = signatureSize() + contentSize + eofBytes() + checksumBytes()

    private fun Slot.asByteArray(): ByteArray {
        val nestBytes = nestService.atSlot(this).map { it.bytes }.orElse(Bytes.emptyBytes())
        val nestBytesSize = nestBytes.size
        val bytes = ByteArray(Integer.BYTES + nestBytesSize)
        copyInt(if (nestBytes.isEmpty) EMPTY_NEST else nestBytesSize, bytes, 0)
        if (!nestBytes.isEmpty) { copyBytes(nestBytes.toByteArray(), bytes, Integer.BYTES, nestBytesSize) }
        return bytes
    }

    private fun PasswordEntry.asByteArray(): ByteArray {
        val keySize = viewKey().size
        val passwordSize = viewPassword().size
        val metaSize = 2 * Integer.BYTES
        val bytes = ByteArray(Integer.BYTES + keySize + passwordSize + metaSize)
        var offset = copyInt(associatedNest().index(), bytes, 0)
        offset += copyInt(keySize, bytes, offset)
        offset += copyBytes(viewKey().toByteArray(), bytes, offset, keySize)
        offset += copyInt(passwordSize, bytes, offset)
        copyBytes(viewPassword().toByteArray(), bytes, offset, passwordSize)
        return bytes
    }
}
