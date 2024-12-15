package de.pflugradts.passbird.adapter.passwordtree

import com.google.inject.Inject
import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.failure.ChecksumFailure
import de.pflugradts.passbird.application.failure.DecryptPasswordTreeFailure
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
import de.pflugradts.passbird.domain.model.egg.Protein.Companion.createProtein
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggStreamSupplier
import de.pflugradts.passbird.domain.service.password.tree.MemoryMap
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory
import java.util.ArrayDeque
import java.util.Arrays

class PasswordTreeReader @Inject constructor(
    private val systemOperation: SystemOperation,
    private val configuration: ReadableConfiguration,
    private val nestService: NestService,
    private val cryptoProvider: CryptoProvider,
) {
    fun restore(): EggStreamSupplier {
        val eggs = ArrayDeque<Egg>()
        val shell = readFromDisk() ?: emptyShell()
        val byteArray = shell.toByteArray()
        if (byteArray.isNotEmpty()) {
            verifySignature(byteArray)
            verifyChecksum(byteArray)
            var offset = signatureSize()
            val memory = if (isPlaceholder(readBytes(byteArray, offset, placeHolder().size))) {
                for (i in 0 until Slot.CAPACITY) {
                    offset += placeHolder().size
                }
                emptyMemory()
            } else {
                val (incrementedOffset, memory) = retrieveMemory(byteArray, offset)
                offset = incrementedOffset
                memory
            }
            offset = populateNests(byteArray, offset)
            while (offset < byteArray.size - 2) {
                val res = byteArray.asEgg(offset)
                eggs.add(res.first)
                offset = res.second
            }
            return EggStreamSupplier({ eggs.stream() }, memory)
        }
        return EggStreamSupplier({ eggs.stream() })
    }

    private fun readFromDisk() =
        tryCatching { systemOperation.readBytesFromFile(filePath).let { cryptoProvider.decrypt(encryptedShellOf(it)) } }
            .onFailure { reportFailure(DecryptPasswordTreeFailure(filePath, it)) }
            .getOrNull()

    private fun verifySignature(bytes: ByteArray) {
        val expectedSignature = signature()
        val actualSignature = ByteArray(signatureSize())
        copyBytes(shellOf(bytes).toByteArray(), actualSignature, 0, signatureSize())
        if (!expectedSignature.contentEquals(actualSignature)) {
            val critical = configuration.adapter.passwordTree.verifySignature
            reportFailure(SignatureCheckFailure(shellOf(actualSignature), critical))
            if (critical) systemOperation.exit()
        }
    }

    private fun verifyChecksum(bytes: ByteArray) {
        val contentSize = calcActualContentSize(bytes.size)
        val expectedChecksum = if (contentSize > 0) checksum(Arrays.copyOfRange(bytes, signatureSize(), contentSize)) else 0x0
        val actualCheckSum = bytes[bytes.size - 1]
        if (expectedChecksum != actualCheckSum) {
            val critical = configuration.adapter.passwordTree.verifyChecksum
            reportFailure(ChecksumFailure(actualCheckSum, expectedChecksum, critical))
            if (critical) systemOperation.exit()
        }
    }

    private fun populateNests(bytes: ByteArray, offset: Int): Int {
        var incrementedOffset = offset
        val nestShells: MutableList<Shell> = ArrayList()
        for (i in 0 until Slot.CAPACITY) {
            bytes.asNestShell(incrementedOffset).let {
                nestShells.add(it.first)
                incrementedOffset += it.second
            }
        }
        nestService.populate(nestShells)
        return incrementedOffset
    }

    private val filePath get() =
        systemOperation.resolvePath(configuration.adapter.passwordTree.location.toDirectory(), PASSWORD_TREE_FILENAME.toFileName())

    private fun calcActualContentSize(totalSize: Int) = totalSize - signatureSize() - checksumBytes()

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
        val proteins = (0..9).map {
            val typeSize = readInt(this, incrementedOffset)
            incrementedOffset += Integer.BYTES
            val typeBytes = if (typeSize > 0) readBytes(this, incrementedOffset, typeSize) else byteArrayOf()
            incrementedOffset += typeSize
            val structureSize = readInt(this, incrementedOffset)
            incrementedOffset += Integer.BYTES
            val structureBytes = if (structureSize > 0) readBytes(this, incrementedOffset, structureSize) else byteArrayOf()
            incrementedOffset += structureSize
            if (typeSize > 0 && structureSize > 0) {
                mutableOptionOf(createProtein(encryptedShellOf(typeBytes), encryptedShellOf(structureBytes)))
            } else {
                mutableOptionOf()
            }
        }.toList()
        return Pair(createEgg(slotAt(nestSlot), encryptedShellOf(eggIdBytes), encryptedShellOf(passwordBytes), proteins), incrementedOffset)
    }

    private fun isPlaceholder(byteArray: ByteArray): Boolean = encryptedShellOf(byteArray) == placeHolder()

    private fun ByteArray.asMemoryEntry(offset: Int): Pair<MutableOption<EncryptedShell>, Int> {
        var incrementedOffset = offset
        val shellSize = readInt(this, incrementedOffset)
        incrementedOffset += Integer.BYTES
        val encryptedShellOption = if (shellSize > 0) {
            val shellBytes = readBytes(this, incrementedOffset, shellSize)
            incrementedOffset += shellSize
            mutableOptionOf(encryptedShellOf(shellBytes))
        } else {
            mutableOptionOf()
        }
        return Pair(encryptedShellOption, incrementedOffset)
    }

    private fun retrieveMemory(byteArray: ByteArray, offset: Int): Pair<Int, MemoryMap> {
        var incrementedOffset = offset
        return (0..Slot.CAPACITY).associate { nestSlot ->
            val slot = slotAt(nestSlot)
            val (list, newOffset) = (0..Slot.CAPACITY).fold(Pair(emptyList<MutableOption<EncryptedShell>>(), incrementedOffset)) { acc, _ ->
                val (currentList, currentOffset) = acc
                val res = byteArray.asMemoryEntry(currentOffset)
                currentList + res.first to res.second
            }
            incrementedOffset = newOffset
            slot to list
        }.let { Pair(incrementedOffset, it) }
    }
}
