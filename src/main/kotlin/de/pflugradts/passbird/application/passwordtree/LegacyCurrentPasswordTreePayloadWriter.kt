package de.pflugradts.passbird.application.passwordtree

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.application.util.copyInt
import de.pflugradts.passbird.application.util.readBytes
import de.pflugradts.passbird.application.util.scramble
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.Protein
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf

class LegacyCurrentPasswordTreePayloadWriter {
    fun write(snapshot: PasswordTreeSnapshot): Shell {
        val contentSize = calcRequiredContentSize(snapshot)
        val bytes = ByteArray(signatureSize() + contentSize + checksumBytes())
        try {
            var offset = copyBytes(signature(), bytes, 0, signatureSize())
            snapshot.memory.forEach { memoryListOption ->
                memoryListOption.ifPresent { memoryList ->
                    memoryList.forEach { memoryEntry ->
                        offset += memoryEntry.encryptedShellAsByteArray().copyToAndScramble(bytes, offset)
                    }
                }
            }
            snapshot.favorites.forEach { favoriteListOption ->
                favoriteListOption.ifPresent { favoriteList ->
                    favoriteList.forEach { favoriteEntry ->
                        offset += favoriteEntry.encryptedShellAsByteArray().copyToAndScramble(bytes, offset)
                    }
                }
            }
            snapshot.nests.forEach { nestShell ->
                offset += nestShell.nestAsByteArray().copyToAndScramble(bytes, offset)
            }
            snapshot.eggs.forEach { egg ->
                offset += egg.eggAsByteArray().copyToAndScramble(bytes, offset)
            }
            val checksumBytes = byteArrayOf(if (contentSize > 0) bytes.contentChecksum(contentSize) else 0x0)
            try {
                copyBytes(checksumBytes, bytes, offset, checksumBytes())
            } finally {
                checksumBytes.scramble()
            }
            return shellOf(bytes)
        } finally {
            bytes.scramble()
        }
    }

    private fun calcRequiredContentSize(snapshot: PasswordTreeSnapshot): Int {
        val memorySize = 100 * Integer.BYTES + snapshot.memory.fold(0) { acc, inner ->
            acc + inner.map { slots -> slots.sumOf { it.map(EncryptedShell::size).orElse(0) } }.orElse(0)
        }
        val favoriteSize = 100 * Integer.BYTES + snapshot.favorites.fold(0) { acc, inner ->
            acc + inner.map { slots -> slots.sumOf { it.map(EncryptedShell::size).orElse(0) } }.orElse(0)
        }
        val eggDataSize = snapshot.eggs.sumOf { it.serializedDataSize() }
        val eggMetaSize = snapshot.eggs.size * 22 * intBytes()
        val nestSize = snapshot.nests.size * intBytes() + snapshot.nests.filter { !it.isEmpty }.sumOf { it.size }
        return memorySize + favoriteSize + eggDataSize + eggMetaSize + nestSize
    }

    private fun Option<EncryptedShell>.encryptedShellAsByteArray() = if (isPresent) {
        val shellBytesSize = get().size
        val bytes = ByteArray(Integer.BYTES + shellBytesSize)
        copyInt(shellBytesSize, bytes, 0)
        get().toByteArray().copyToAndScramble(bytes, Integer.BYTES)
        bytes
    } else {
        val bytes = ByteArray(Integer.BYTES)
        copyInt(0, bytes, 0)
        bytes
    }

    private fun Shell.nestAsByteArray(): ByteArray {
        val nestBytesSize = size
        val bytes = ByteArray(Integer.BYTES + nestBytesSize)
        copyInt(nestBytesSize, bytes, 0)
        if (!isEmpty) toByteArray().copyToAndScramble(bytes, Integer.BYTES)
        return bytes
    }

    private fun Egg.eggAsByteArray(): ByteArray {
        val eggIdShell = viewEggId()
        val passwordShell = viewPassword()
        val eggIdSize = eggIdShell.size
        val passwordSize = passwordShell.size
        val metaSize = 22 * Integer.BYTES
        val proteinDataSize = proteins.filter { it.isPresent }.sumOf { it.get().serializedDataSize() }
        val bytes = ByteArray(Integer.BYTES + eggIdSize + passwordSize + metaSize + proteinDataSize)
        var completed = false
        try {
            var offset = copyInt(associatedNest().index(), bytes, 0)
            offset += copyInt(eggIdSize, bytes, offset)
            offset += eggIdShell.toByteArray().copyToAndScramble(bytes, offset)
            offset += copyInt(passwordSize, bytes, offset)
            offset += passwordShell.toByteArray().copyToAndScramble(bytes, offset)
            proteins.forEach { slottedProtein ->
                if (slottedProtein.isPresent) {
                    val typeShell = slottedProtein.get().viewType()
                    val structureShell = slottedProtein.get().viewStructure()
                    try {
                        offset += copyInt(typeShell.size, bytes, offset)
                        offset += typeShell.toByteArray().copyToAndScramble(bytes, offset)
                        offset += copyInt(structureShell.size, bytes, offset)
                        offset += structureShell.toByteArray().copyToAndScramble(bytes, offset)
                    } finally {
                        typeShell.scramble()
                        structureShell.scramble()
                    }
                } else {
                    offset += copyInt(0, bytes, offset)
                    offset += copyInt(0, bytes, offset)
                }
            }
            completed = true
            return bytes
        } finally {
            eggIdShell.scramble()
            passwordShell.scramble()
            if (!completed) bytes.scramble()
        }
    }

    private fun Egg.serializedDataSize(): Int {
        val eggIdShell = viewEggId()
        val passwordShell = viewPassword()
        try {
            return intBytes() + eggIdShell.size + passwordShell.size + proteins.filter { it.isPresent }.sumOf {
                it.get().serializedDataSize()
            }
        } finally {
            eggIdShell.scramble()
            passwordShell.scramble()
        }
    }

    private fun Protein.serializedDataSize(): Int {
        val typeShell = viewType()
        val structureShell = viewStructure()
        try {
            return typeShell.size + structureShell.size
        } finally {
            typeShell.scramble()
            structureShell.scramble()
        }
    }

    private fun ByteArray.copyToAndScramble(target: ByteArray, offset: Int) = try {
        copyBytes(this, target, offset, size)
    } finally {
        scramble()
    }

    private fun ByteArray.contentChecksum(contentSize: Int): Byte {
        val checksumSource = readBytes(this, signatureSize(), contentSize)
        try {
            return checksum(checksumSource)
        } finally {
            checksumSource.scramble()
        }
    }
}
