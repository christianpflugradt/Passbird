package de.pflugradts.passbird.application.passwordtree

import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.failure.ChecksumFailure
import de.pflugradts.passbird.application.failure.SignatureCheckFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.FAILURE_EXIT_STATUS
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.readBytes
import de.pflugradts.passbird.application.util.readInt
import de.pflugradts.passbird.application.util.scramble
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.egg.MemoryMap
import de.pflugradts.passbird.domain.model.egg.Protein
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

class LegacyPasswordTreePayloadReader @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val systemOperation: SystemOperation,
) {
    fun read(shell: Shell): PasswordTreeSnapshot {
        val byteArray = shell.toByteArray()
        try {
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
        } finally {
            byteArray.scramble()
        }
    }

    private fun verifySignature(bytes: ByteArray) {
        val expectedSignature = legacySignature()
        val actualSignature = readBytes(bytes, 0, signatureSize())
        try {
            if (!expectedSignature.contentEquals(actualSignature)) {
                val critical = configuration.adapter.passwordTree.verifySignature
                val actualSignatureShell = shellOf(actualSignature)
                try {
                    reportFailure(SignatureCheckFailure(actualSignatureShell, critical))
                } finally {
                    actualSignatureShell.scramble()
                }
                if (critical) systemOperation.exit(FAILURE_EXIT_STATUS)
            }
        } finally {
            actualSignature.scramble()
        }
    }

    private fun verifyChecksum(bytes: ByteArray) {
        val contentSize = calcActualContentSize(bytes.size)
        val expectedChecksum = if (contentSize > 0) {
            val checksumSource = readBytes(bytes, signatureSize(), contentSize)
            try {
                checksum(checksumSource)
            } finally {
                checksumSource.scramble()
            }
        } else {
            0x0
        }
        val actualCheckSum = bytes[bytes.size - 1]
        if (expectedChecksum != actualCheckSum) {
            val critical = configuration.adapter.passwordTree.verifyChecksum
            reportFailure(ChecksumFailure(actualCheckSum, expectedChecksum, critical))
            if (critical) systemOperation.exit(FAILURE_EXIT_STATUS)
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
            nestBytes.toShellAndScramble()
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
        val eggIdShell = readBytes(this, incrementedOffset, eggIdSize).toEncryptedShellAndScramble()
        incrementedOffset += eggIdSize
        val passwordSize = readInt(this, incrementedOffset)
        incrementedOffset += Integer.BYTES
        val passwordShell = readBytes(this, incrementedOffset, passwordSize).toEncryptedShellAndScramble()
        incrementedOffset += passwordSize
        try {
            val proteins = (0..9).map {
                val typeSize = readInt(this, incrementedOffset)
                incrementedOffset += Integer.BYTES
                val typeBytes = if (typeSize > 0) readBytes(this, incrementedOffset, typeSize) else byteArrayOf()
                incrementedOffset += typeSize
                val structureSize = readInt(this, incrementedOffset)
                incrementedOffset += Integer.BYTES
                val structureBytes = if (structureSize > 0) readBytes(this, incrementedOffset, structureSize) else byteArrayOf()
                incrementedOffset += structureSize
                proteinOption(typeBytes, structureBytes)
            }
            return Pair(createEgg(slotAt(nestSlot), eggIdShell, passwordShell, proteins), incrementedOffset)
        } finally {
            eggIdShell.scramble()
            passwordShell.scramble()
        }
    }

    private fun proteinOption(typeBytes: ByteArray, structureBytes: ByteArray): MutableOption<Protein> {
        if (typeBytes.isEmpty() || structureBytes.isEmpty()) {
            typeBytes.scramble()
            structureBytes.scramble()
            return mutableOptionOf()
        }
        val typeShell = typeBytes.toEncryptedShellAndScramble()
        val structureShell = structureBytes.toEncryptedShellAndScramble()
        try {
            return mutableOptionOf(createProtein(typeShell, structureShell))
        } finally {
            typeShell.scramble()
            structureShell.scramble()
        }
    }

    private fun isPlaceholder(byteArray: ByteArray): Boolean {
        val encryptedShell = byteArray.toEncryptedShellAndScramble()
        try {
            return encryptedShell == placeHolder()
        } finally {
            encryptedShell.scramble()
        }
    }

    private fun ByteArray.asMemoryEntry(offset: Int): Pair<MutableOption<EncryptedShell>, Int> {
        var incrementedOffset = offset
        val shellSize = readInt(this, incrementedOffset)
        incrementedOffset += Integer.BYTES
        val encryptedShellOption = if (shellSize > 0) {
            val shellBytes = readBytes(this, incrementedOffset, shellSize)
            incrementedOffset += shellSize
            mutableOptionOf(shellBytes.toEncryptedShellAndScramble())
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

    private fun ByteArray.toShellAndScramble() = try {
        shellOf(this)
    } finally {
        scramble()
    }

    private fun ByteArray.toEncryptedShellAndScramble() = try {
        encryptedShellOf(this)
    } finally {
        scramble()
    }
}
