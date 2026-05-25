package de.pflugradts.passbird.application.passwordtree

import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.failure.ChecksumFailure
import de.pflugradts.passbird.application.failure.SignatureCheckFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.application.util.readBytes
import de.pflugradts.passbird.application.util.readInt
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.egg.MemoryMap
import de.pflugradts.passbird.domain.model.egg.Protein.Companion.createProtein
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.slot.Slots
import de.pflugradts.passbird.domain.model.slot.Slots.Companion.slotIterator
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory
import jakarta.inject.Inject
import java.util.ArrayDeque
import java.util.Arrays

class PasswordTreePayloadReader @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val systemOperation: SystemOperation,
) {
    fun read(shell: Shell): PasswordTreeSnapshot {
        val byteArray = shell.toByteArray()
        if (byteArray.isEmpty()) {
            return PasswordTreeSnapshot()
        }
        verifySignature(byteArray)
        verifyChecksum(byteArray)
        var offset = signatureSize()
        val memory = if (isPlaceholder(readBytes(byteArray, offset, placeHolder().size))) {
            repeat(Slot.CAPACITY) {
                offset += placeHolder().size
            }
            emptyMemory()
        } else {
            val (incrementedOffset, retrievedMemory) = retrieveMemory(byteArray, offset)
            offset = incrementedOffset
            if (eggIdMemoryEnabled) retrievedMemory else emptyMemory()
        }
        val (nests, incrementedOffset) = retrieveNests(byteArray, offset)
        offset = incrementedOffset
        val eggs = ArrayDeque<Egg>()
        while (offset < byteArray.size - checksumBytes()) {
            val (egg, newOffset) = byteArray.asEgg(offset)
            eggs.add(egg)
            offset = newOffset
        }
        return PasswordTreeSnapshot(eggs = eggs.toList(), memory = memory, nests = nests)
    }

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
        val expectedChecksum = if (contentSize > 0) {
            checksum(Arrays.copyOfRange(bytes, signatureSize(), signatureSize() + contentSize))
        } else {
            0x0
        }
        val actualCheckSum = bytes[bytes.size - 1]
        if (expectedChecksum != actualCheckSum) {
            val critical = configuration.adapter.passwordTree.verifyChecksum
            reportFailure(ChecksumFailure(actualCheckSum, expectedChecksum, critical))
            if (critical) systemOperation.exit()
        }
    }

    private fun retrieveNests(bytes: ByteArray, offset: Int): Pair<List<Shell>, Int> {
        var incrementedOffset = offset
        val nests = ArrayList<Shell>(Slot.CAPACITY)
        repeat(Slot.CAPACITY) {
            val (nestShell, consumedBytes) = bytes.asNestShell(incrementedOffset)
            nests.add(nestShell)
            incrementedOffset += consumedBytes
        }
        return Pair(nests, incrementedOffset)
    }

    private val eggIdMemoryEnabled get() = with(configuration.domain.eggIdMemory) { enabled && persisted }

    private fun calcActualContentSize(totalSize: Int) = totalSize - signatureSize() - checksumBytes()

    private fun ByteArray.asNestShell(offset: Int): Pair<Shell, Int> {
        var incrementedOffset = offset
        val nestSize = readInt(this, incrementedOffset)
        incrementedOffset += Integer.BYTES
        val result = if (nestSize > 0) {
            val nestBytes = readBytes(this, incrementedOffset, nestSize)
            incrementedOffset += nestBytes.size
            shellOf(nestBytes)
        } else {
            Shell.emptyShell()
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
        }
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
        return Slots<EggIdMemory>().apply {
            slotIterator().forEach { nestSlot ->
                this[nestSlot].set(
                    EggIdMemory().apply {
                        slotIterator().forEach { slot ->
                            val (entry, newOffset) = byteArray.asMemoryEntry(incrementedOffset)
                            entry.ifPresent { this[slot].set(it) }
                            incrementedOffset = newOffset
                        }
                    },
                )
            }
        }.let { Pair(incrementedOffset, it) }
    }
}
