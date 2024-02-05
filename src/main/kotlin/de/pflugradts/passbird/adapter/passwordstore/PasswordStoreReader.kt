package de.pflugradts.passbird.adapter.passwordstore

import com.google.inject.Inject
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.DATABASE_FILENAME
import de.pflugradts.passbird.application.failure.ChecksumFailure
import de.pflugradts.passbird.application.failure.DecryptPasswordDatabaseFailure
import de.pflugradts.passbird.application.failure.SignatureCheckFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.application.util.readBytes
import de.pflugradts.passbird.application.util.readInt
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggStreamSupplier
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
    fun restore(): EggStreamSupplier {
        val eggs = ArrayDeque<Egg>()
        val shell = readFromDisk() ?: emptyShell()
        val byteArray = shell.toByteArray()
        if (byteArray.isNotEmpty()) {
            verifySignature(byteArray)
            verifyChecksum(byteArray)
            var offset = signatureSize()
            offset = populateNests(byteArray, offset)
            while (EOF != readInt(byteArray, offset)) {
                val res = byteArray.asEgg(offset)
                if (cryptoProvider.decrypt(res.first.viewEggId()).slice(0, 1).asString() !in listOf("1", "2")) eggs.add(res.first)
                offset = res.second
            }
            return Supplier { eggs.stream() }
        }
        return Supplier { Stream.empty() }
    }

    private fun readFromDisk() =
        tryCatching { systemOperation.readBytesFromFile(filePath).let { if (it.isEmpty) it else cryptoProvider.decrypt(it) } }
            .onFailure { reportFailure(DecryptPasswordDatabaseFailure(filePath, it)) }
            .getOrNull()

    private fun verifySignature(bytes: ByteArray) {
        val expectedSignature = signature()
        val actualSignature = ByteArray(signatureSize())
        copyBytes(shellOf(bytes).toByteArray(), actualSignature, 0, signatureSize())
        if (!expectedSignature.contentEquals(actualSignature)) {
            val critical = configuration.adapter.passwordStore.verifySignature
            reportFailure(SignatureCheckFailure(shellOf(actualSignature), critical))
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
            val nestShells: MutableList<Shell> = ArrayList()
            for (i in 0 until Slot.CAPACITY) {
                bytes.asNestShell(incrementedOffset).let {
                    nestShells.add(it.first)
                    incrementedOffset += it.second
                }
            }
            nestService.populate(nestShells)
        }
        return incrementedOffset
    }

    private val filePath get() =
        systemOperation.resolvePath(configuration.adapter.passwordStore.location.toDirectory(), DATABASE_FILENAME.toFileName())
    private fun calcActualContentSize(totalSize: Int) = totalSize - signatureSize() - checksumBytes() - eofBytes()

    private fun ByteArray.asNestShell(offset: Int): Pair<Shell, Int> {
        var incrementedOffset = offset
        val nestSize = readInt(this, incrementedOffset)
        incrementedOffset += Integer.BYTES
        val result: Shell
        if (nestSize > 0) {
            val nestBytes = readBytes(this, incrementedOffset, nestSize)
            incrementedOffset += nestBytes.size
            result = shellOf(nestBytes)
        } else {
            result = emptyShell()
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
            createEgg(Slot.slotAt(nestSlot), shellOf(eggIdBytes), shellOf(passwordBytes)),
            incrementedOffset,
        )
    }
}
