package de.pflugradts.passbird.adapter.passwordstore

import com.google.inject.Inject
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.DATABASE_FILENAME
import de.pflugradts.passbird.application.failure.ChecksumFailure
import de.pflugradts.passbird.application.failure.DecryptPasswordDatabaseFailure
import de.pflugradts.passbird.application.failure.SignatureCheckFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.application.util.readBytes
import de.pflugradts.passbird.application.util.readInt
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import java.util.ArrayDeque
import java.util.Arrays
import java.util.function.Supplier
import java.util.stream.Stream

private const val EOF = 0
private const val SECTOR = -1

class PasswordStoreReader @Inject constructor(
    @Inject private val systemOperation: SystemOperation,
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val nestService: NestService,
    @Inject private val cryptoProvider: CryptoProvider,
) {
    fun restore(): Supplier<Stream<Egg>> {
        val eggs = ArrayDeque<Egg>()
        val bytes = readFromDisk() ?: emptyBytes()
        val byteArray = bytes.toByteArray()
        if (byteArray.isNotEmpty()) {
            verifySignature(byteArray)
            verifyChecksum(byteArray)
            var offset = signatureSize()
            offset = populateNests(byteArray, offset)
            while (EOF != readInt(byteArray, offset)) {
                val res = byteArray.asEgg(offset)
                eggs.add(res.first)
                offset = res.second
            }
            return Supplier { eggs.stream() }
        }
        return Supplier { Stream.empty() }
    }

    private fun readFromDisk() =
        tryCatching { systemOperation.readBytesFromFile(filePath!!).let { if (it.isEmpty) it else cryptoProvider.decrypt(it) } }
            .onFailure { reportFailure(DecryptPasswordDatabaseFailure(filePath!!, it)) }
            .getOrNull()

    private fun verifySignature(bytes: ByteArray) {
        val expectedSignature = signature()
        val actualSignature = ByteArray(signatureSize())
        copyBytes(bytesOf(bytes).toByteArray(), actualSignature, 0, signatureSize())
        if (!expectedSignature.contentEquals(actualSignature)) {
            val critical = configuration.adapter.passwordStore.verifySignature
            reportFailure(SignatureCheckFailure(bytesOf(actualSignature), critical))
            if (critical) systemOperation.exit()
        }
    }

    private fun verifyChecksum(bytes: ByteArray) {
        val contentSize = calcActualContentSize(bytes.size)
        val expectedChecksum = if (contentSize > 0) checksum(Arrays.copyOfRange(bytes, signatureSize(), contentSize)) else 0x0
        val actualCheckSum = bytes[bytes.size - 1]
        if (expectedChecksum != actualCheckSum) {
            val critical = configuration.adapter.passwordStore.verifyChecksum
            reportFailure(ChecksumFailure(actualCheckSum, expectedChecksum, critical))
            if (critical) systemOperation.exit()
        }
    }

    private fun populateNests(bytes: ByteArray, offset: Int): Int {
        var incrementedOffset = offset
        if (SECTOR == readInt(bytes, incrementedOffset)) {
            incrementedOffset += intBytes()
            val nestBytes: MutableList<Bytes> = ArrayList()
            for (i in 0 until Slot.CAPACITY) {
                bytes.asNestBytes(incrementedOffset).let {
                    nestBytes.add(it.first)
                    incrementedOffset += it.second
                }
            }
            nestService.populate(nestBytes)
        }
        return incrementedOffset
    }

    private val filePath get() = systemOperation.resolvePath(configuration.adapter.passwordStore.location, DATABASE_FILENAME)
    private fun calcActualContentSize(totalSize: Int) = totalSize - signatureSize() - checksumBytes() - eofBytes()

    private fun ByteArray.asNestBytes(offset: Int): Pair<Bytes, Int> {
        var incrementedOffset = offset
        val nestSize = readInt(this, incrementedOffset)
        incrementedOffset += Integer.BYTES
        val result: Bytes
        if (nestSize > 0) {
            val nestBytes = readBytes(this, incrementedOffset, nestSize)
            incrementedOffset += nestBytes.size
            result = bytesOf(nestBytes)
        } else {
            result = Bytes.emptyBytes()
        }
        return Pair(result, incrementedOffset - offset)
    }

    private fun ByteArray.asEgg(offset: Int): Pair<Egg, Int> {
        var incrementedOffset = offset
        val nestSlot = readInt(this, incrementedOffset)
        incrementedOffset += Integer.BYTES
        val eggIdSize = readInt(this, incrementedOffset)
        incrementedOffset += Integer.BYTES
        val eggIdBytes = readBytes(this, incrementedOffset, eggIdSize)
        incrementedOffset += eggIdSize
        val passwordSize = readInt(this, incrementedOffset)
        incrementedOffset += Integer.BYTES
        val passwordBytes = readBytes(this, incrementedOffset, passwordSize)
        incrementedOffset += passwordSize
        return Pair(
            createEgg(Slot.at(nestSlot), bytesOf(eggIdBytes), bytesOf(passwordBytes)),
            incrementedOffset,
        )
    }
}
