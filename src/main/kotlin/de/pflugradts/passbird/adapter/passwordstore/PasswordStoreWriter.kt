package de.pflugradts.passbird.adapter.passwordstore

import com.google.inject.Inject
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.DATABASE_FILENAME
import de.pflugradts.passbird.application.failure.WritePasswordDatabaseFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.application.util.copyInt
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggStreamSupplier
import java.util.Arrays

private const val EOF = 0
private const val SECTOR = -1
private const val EMPTY_NEST = -2

class PasswordStoreWriter @Inject constructor(
    @Inject private val systemOperation: SystemOperation,
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val nestService: NestService,
    @Inject private val cryptoProvider: CryptoProvider,
) {

    fun sync(eggSupplier: EggStreamSupplier) {
        val contentSize = calcRequiredContentSize(eggSupplier)
        val bytes = ByteArray(calcActualTotalSize(contentSize))
        var offset = copyBytes(signature(), bytes, 0, signatureSize())
        offset = persistNest(bytes, offset)
        eggSupplier.get().forEach { egg -> egg.asByteArray().let { offset += copyBytes(it, bytes, offset, it.size) } }
        offset += copyInt(EOF, bytes, offset)
        val checksumBytes = byteArrayOf(if (contentSize > 0) checksum(Arrays.copyOfRange(bytes, signatureSize(), contentSize)) else 0x0)
        copyBytes(checksumBytes, bytes, offset, checksumBytes())
        writeToDisk(shellOf(bytes))
    }

    private fun persistNest(bytes: ByteArray, offset: Int): Int {
        var incrementedOffset = offset
        copyInt(SECTOR, bytes, incrementedOffset)
        incrementedOffset += intBytes()
        for (index in NestSlot.FIRST_SLOT..NestSlot.LAST_SLOT) {
            NestSlot.at(index).asByteArray().let { incrementedOffset += copyBytes(it, bytes, incrementedOffset, it.size) }
        }
        return incrementedOffset
    }

    private fun writeToDisk(shell: Shell) =
        tryCatching { systemOperation.writeBytesToFile(filePath, cryptoProvider.encrypt(shell)) }
            .onFailure { reportFailure(WritePasswordDatabaseFailure(filePath, it)) }

    private fun calcRequiredContentSize(eggs: EggStreamSupplier): Int {
        val dataSize = eggs.get()
            .map { egg: Egg -> intBytes() + egg.viewEggId().size + egg.viewPassword().size }
            .reduce(0) { a: Int, b: Int -> Integer.sum(a, b) }
        val nestSize = intBytes() + NestSlot.CAPACITY * intBytes() + nestService.all()
            .filter { it.isPresent }
            .map { it.get().shell.size }
            .reduce(0) { a: Int, b: Int -> Integer.sum(a, b) }
        val metaSize = eggs.get().count().toInt() * 2 * intBytes()
        return dataSize + nestSize + metaSize
    }

    private val filePath get() =
        systemOperation.resolvePath(configuration.adapter.passwordStore.location.toDirectory(), DATABASE_FILENAME.toFileName())
    private fun calcActualTotalSize(contentSize: Int) = signatureSize() + contentSize + eofBytes() + checksumBytes()

    private fun NestSlot.asByteArray(): ByteArray {
        val nestShell = nestService.atNestSlot(this).map { it.shell }.orElse(emptyShell())
        val nestBytesSize = nestShell.size
        val bytes = ByteArray(Integer.BYTES + nestBytesSize)
        copyInt(if (nestShell.isEmpty) EMPTY_NEST else nestBytesSize, bytes, 0)
        if (!nestShell.isEmpty) { copyBytes(nestShell.toByteArray(), bytes, Integer.BYTES, nestBytesSize) }
        return bytes
    }

    private fun Egg.asByteArray(): ByteArray {
        val eggIdSize = viewEggId().size
        val passwordSize = viewPassword().size
        val metaSize = 2 * Integer.BYTES
        val bytes = ByteArray(Integer.BYTES + eggIdSize + passwordSize + metaSize)
        var offset = copyInt(associatedNest().index(), bytes, 0)
        offset += copyInt(eggIdSize, bytes, offset)
        offset += copyBytes(viewEggId().toByteArray(), bytes, offset, eggIdSize)
        offset += copyInt(passwordSize, bytes, offset)
        copyBytes(viewPassword().toByteArray(), bytes, offset, passwordSize)
        return bytes
    }
}
