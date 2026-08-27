package de.pflugradts.passbird.application.passwordtree
import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.failure.ChecksumFailure
import de.pflugradts.passbird.application.failure.SignatureCheckFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.FAILURE_EXIT_STATUS
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.scramble
import de.pflugradts.passbird.application.yolk.totpAlgorithmAtOrdinal
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.EggIdFavorites
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.egg.FavoriteMap
import de.pflugradts.passbird.domain.model.egg.MemoryMap
import de.pflugradts.passbird.domain.model.egg.Protein
import de.pflugradts.passbird.domain.model.egg.Protein.Companion.createProtein
import de.pflugradts.passbird.domain.model.egg.Yolk
import de.pflugradts.passbird.domain.model.egg.Yolk.Companion.createYolk
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slots
import de.pflugradts.passbird.domain.model.slot.Slots.Companion.slotIterator
import de.pflugradts.passbird.domain.service.password.tree.emptyFavorites
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory
import java.util.ArrayDeque
class PasswordTreePayloadReader constructor(
    private val configuration: ReadableConfiguration,
    private val systemOperation: SystemOperation,
) {
    fun read(shell: Shell): PasswordTreeSnapshot {
        val byteArray = shell.toByteArray()
        try {
            if (byteArray.isEmpty()) {
                return PasswordTreeSnapshot()
            }
            validatePayloadEnvelope(byteArray)
            if (!verifySignature(byteArray) || !verifyChecksum(byteArray)) {
                return PasswordTreeSnapshot()
            }
            var offset = signatureSize()
            val payloadVersion = readPayloadInt(byteArray, offset, payloadContentEnd(byteArray)).let {
                if (it == currentPasswordTreePayloadFormatMarker()) {
                    offset += Integer.BYTES
                    readPayloadInt(byteArray, offset, payloadContentEnd(byteArray)).also { version ->
                        offset += Integer.BYTES
                        if (version != currentPasswordTreePayloadVersion()) {
                            throwUnsupportedPasswordTreeFormat()
                        }
                    }
                } else {
                    0
                }
            }
            val payloadEnd = payloadContentEnd(byteArray)
            val memory = if (isPlaceholder(readPayloadBytes(byteArray, offset, placeHolder().size, payloadEnd))) {
                validatePayloadRange(byteArray, offset, Slot.CAPACITY * placeHolder().size, payloadEnd)
                repeat(Slot.CAPACITY) {
                    offset += placeHolder().size
                }
                emptyMemory()
            } else {
                val (incrementedOffset, retrievedMemory) = retrieveMemory(byteArray, offset)
                offset = incrementedOffset
                if (eggIdMemoryEnabled) retrievedMemory else emptyMemory()
            }
            val (incrementedFavoriteOffset, favorites) = retrieveFavorites(byteArray, offset)
            offset = incrementedFavoriteOffset
            val (nests, incrementedNestOffset) = retrieveNests(byteArray, offset)
            offset = incrementedNestOffset
            val eggs = ArrayDeque<Egg>()
            while (offset < payloadEnd) {
                val (egg, newOffset) = byteArray.asEgg(offset, payloadVersion >= currentPasswordTreePayloadVersion())
                eggs.add(egg)
                offset = newOffset
            }
            validatePayloadEnd(offset, payloadEnd)
            return PasswordTreeSnapshot(eggs = eggs.toList(), favorites = favorites, memory = memory, nests = nests)
        } finally {
            byteArray.scramble()
        }
    }
    private fun verifySignature(bytes: ByteArray): Boolean {
        val expectedSignature = signature()
        val actualSignature = readPayloadBytes(bytes, 0, signatureSize())
        try {
            if (!expectedSignature.contentEquals(actualSignature)) {
                val critical = configuration.adapter.passwordTree.verifySignature
                val actualSignatureShell = shellOf(actualSignature)
                try {
                    reportFailure(SignatureCheckFailure(actualSignatureShell, critical))
                } finally {
                    actualSignatureShell.scramble()
                }
                if (critical) {
                    systemOperation.exit(FAILURE_EXIT_STATUS)
                    return false
                }
            }
            return true
        } finally {
            actualSignature.scramble()
        }
    }
    private fun verifyChecksum(bytes: ByteArray): Boolean {
        val contentSize = calcActualContentSize(bytes.size)
        val expectedChecksum = if (contentSize > 0) {
            val checksumSource = readPayloadBytes(bytes, signatureSize(), contentSize, payloadContentEnd(bytes))
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
            if (critical) {
                systemOperation.exit(FAILURE_EXIT_STATUS)
                return false
            }
        }
        return true
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
        val payloadEnd = payloadContentEnd(this)
        var incrementedOffset = offset
        val nestSize = readPayloadSize(this, incrementedOffset, payloadEnd)
        incrementedOffset += Integer.BYTES
        val result = if (nestSize > 0) {
            val nestBytes = readPayloadBytes(this, incrementedOffset, nestSize, payloadEnd)
            incrementedOffset += nestBytes.size
            nestBytes.toShellAndScramble()
        } else {
            Shell.emptyShell()
        }
        return Pair(result, incrementedOffset - offset)
    }
    private fun ByteArray.asEgg(offset: Int, hasTrashMetadata: Boolean): Pair<Egg, Int> {
        val payloadEnd = payloadContentEnd(this)
        var incrementedOffset = offset
        val nestSlot = storedEggNestSlot(readPayloadInt(this, incrementedOffset, payloadEnd))
        incrementedOffset += Integer.BYTES
        val trashed = if (hasTrashMetadata) {
            readPayloadInt(this, incrementedOffset, payloadEnd).let {
                incrementedOffset += Integer.BYTES
                storedTrashFlag(it)
            }
        } else {
            false
        }
        val deletionEpochDay = if (hasTrashMetadata) {
            readPayloadInt(this, incrementedOffset, payloadEnd).also {
                incrementedOffset += Integer.BYTES
                require(it >= 0) { throwUnsupportedPasswordTreeFormat() }
            }
        } else {
            0
        }
        val eggIdSize = readPayloadSize(this, incrementedOffset, payloadEnd)
        incrementedOffset += Integer.BYTES
        val eggIdShell = readPayloadBytes(this, incrementedOffset, eggIdSize, payloadEnd).toEncryptedShellAndScramble()
        incrementedOffset += eggIdSize
        val passwordSize = readPayloadSize(this, incrementedOffset, payloadEnd)
        incrementedOffset += Integer.BYTES
        val passwordShell = readPayloadBytes(this, incrementedOffset, passwordSize, payloadEnd).toEncryptedShellAndScramble()
        incrementedOffset += passwordSize
        try {
            val proteins = (0..9).map {
                val typeSize = readPayloadSize(this, incrementedOffset, payloadEnd)
                incrementedOffset += Integer.BYTES
                val typeBytes = if (typeSize > 0) readPayloadBytes(this, incrementedOffset, typeSize, payloadEnd) else byteArrayOf()
                incrementedOffset += typeSize
                val structureSize = readPayloadSize(this, incrementedOffset, payloadEnd)
                incrementedOffset += Integer.BYTES
                val structureBytes = if (structureSize > 0) {
                    readPayloadBytes(this, incrementedOffset, structureSize, payloadEnd)
                } else {
                    byteArrayOf()
                }
                incrementedOffset += structureSize
                proteinOption(typeBytes, structureBytes)
            }
            val yolk = yolkOption(this, incrementedOffset, payloadEnd)
            incrementedOffset = yolk.second
            return Pair(createEgg(nestSlot, eggIdShell, passwordShell, proteins, yolk.first, trashed, deletionEpochDay), incrementedOffset)
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
    private fun yolkOption(bytes: ByteArray, offset: Int, payloadEnd: Int): Pair<MutableOption<Yolk>, Int> {
        var incrementedOffset = offset
        val secretSize = readPayloadSize(bytes, incrementedOffset, payloadEnd)
        incrementedOffset += Integer.BYTES
        if (secretSize <= 0) {
            repeat(3) {
                readPayloadInt(bytes, incrementedOffset, payloadEnd)
                incrementedOffset += Integer.BYTES
            }
            return Pair(mutableOptionOf(), incrementedOffset)
        }
        val secretShell = readPayloadBytes(bytes, incrementedOffset, secretSize, payloadEnd).toEncryptedShellAndScramble()
        incrementedOffset += secretSize
        val algorithmOrdinal = readPayloadInt(bytes, incrementedOffset, payloadEnd)
        incrementedOffset += Integer.BYTES
        val digits = readPayloadInt(bytes, incrementedOffset, payloadEnd)
        incrementedOffset += Integer.BYTES
        val periodSeconds = readPayloadInt(bytes, incrementedOffset, payloadEnd)
        incrementedOffset += Integer.BYTES
        return try {
            val algorithm = totpAlgorithmAtOrdinal(algorithmOrdinal)
            Pair(mutableOptionOf(createYolk(secretShell, algorithm, digits, periodSeconds)), incrementedOffset)
        } finally {
            secretShell.scramble()
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
        val payloadEnd = payloadContentEnd(this)
        var incrementedOffset = offset
        val shellSize = readPayloadSize(this, incrementedOffset, payloadEnd)
        incrementedOffset += Integer.BYTES
        val encryptedShellOption = if (shellSize > 0) {
            val shellBytes = readPayloadBytes(this, incrementedOffset, shellSize, payloadEnd)
            incrementedOffset += shellSize
            mutableOptionOf(shellBytes.toEncryptedShellAndScramble())
        } else {
            mutableOptionOf()
        }
        return Pair(encryptedShellOption, incrementedOffset)
    }
    private fun retrieveFavorites(byteArray: ByteArray, offset: Int): Pair<Int, FavoriteMap> {
        var incrementedOffset = offset
        return Slots<EggIdFavorites>().apply {
            slotIterator().forEach { nestSlot ->
                this[nestSlot].set(
                    EggIdFavorites().apply {
                        slotIterator().forEach { slot ->
                            val (entry, newOffset) = byteArray.asMemoryEntry(incrementedOffset)
                            entry.ifPresent {
                                try {
                                    assign(slot, it)
                                } finally {
                                    it.scramble()
                                }
                            }
                            incrementedOffset = newOffset
                        }
                    },
                )
            }
        }.let { Pair(incrementedOffset, it) }
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
