package de.pflugradts.passbird.application.passwordtree

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.application.util.copyInt
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import jakarta.inject.Inject
import java.util.Arrays

class PasswordTreePayloadWriter @Inject constructor() {
    fun write(snapshot: PasswordTreeSnapshot): Shell {
        val contentSize = calcRequiredContentSize(snapshot)
        val bytes = ByteArray(signatureSize() + contentSize + checksumBytes())
        var offset = copyBytes(signature(), bytes, 0, signatureSize())
        snapshot.memory.forEach { memoryListOption ->
            memoryListOption.ifPresent { memoryList ->
                memoryList.forEach { memoryEntry ->
                    memoryEntry.encryptedShellAsByteArray().let { offset += copyBytes(it, bytes, offset, it.size) }
                }
            }
        }
        snapshot.favorites.forEach { favoriteListOption ->
            favoriteListOption.ifPresent { favoriteList ->
                favoriteList.forEach { favoriteEntry ->
                    favoriteEntry.encryptedShellAsByteArray().let { offset += copyBytes(it, bytes, offset, it.size) }
                }
            }
        }
        snapshot.nests.forEach { nestShell ->
            nestShell.nestAsByteArray().let { offset += copyBytes(it, bytes, offset, it.size) }
        }
        snapshot.eggs.forEach { egg ->
            egg.eggAsByteArray().let { offset += copyBytes(it, bytes, offset, it.size) }
        }
        val checksumBytes = byteArrayOf(
            if (contentSize > 0) checksum(Arrays.copyOfRange(bytes, signatureSize(), signatureSize() + contentSize)) else 0x0,
        )
        copyBytes(checksumBytes, bytes, offset, checksumBytes())
        return shellOf(bytes)
    }

    private fun calcRequiredContentSize(snapshot: PasswordTreeSnapshot): Int {
        val memorySize = 100 * Integer.BYTES + snapshot.memory.fold(0) { acc, inner ->
            acc + inner.map { slots -> slots.sumOf { it.map(EncryptedShell::size).orElse(0) } }.orElse(0)
        }
        val favoriteSize = 100 * Integer.BYTES + snapshot.favorites.fold(0) { acc, inner ->
            acc + inner.map { slots -> slots.sumOf { it.map(EncryptedShell::size).orElse(0) } }.orElse(0)
        }
        val eggDataSize = snapshot.eggs.sumOf { egg ->
            intBytes() + egg.viewEggId().size + egg.viewPassword().size + egg.proteins.filter { it.isPresent }.sumOf {
                it.get().viewType().size + it.get().viewStructure().size
            }
        }
        val eggMetaSize = snapshot.eggs.size * 22 * intBytes()
        val nestSize = snapshot.nests.size * intBytes() + snapshot.nests.filter { !it.isEmpty }.sumOf { it.size }
        return memorySize + favoriteSize + eggDataSize + eggMetaSize + nestSize
    }

    private fun Option<EncryptedShell>.encryptedShellAsByteArray() = if (isPresent) {
        val shellBytesSize = get().size
        val bytes = ByteArray(Integer.BYTES + shellBytesSize)
        copyInt(shellBytesSize, bytes, 0)
        copyBytes(get().toByteArray(), bytes, Integer.BYTES, shellBytesSize)
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
        if (!isEmpty) copyBytes(toByteArray(), bytes, Integer.BYTES, nestBytesSize)
        return bytes
    }

    private fun Egg.eggAsByteArray(): ByteArray {
        val eggIdSize = viewEggId().size
        val passwordSize = viewPassword().size
        val metaSize = 22 * Integer.BYTES
        val bytes = ByteArray(
            Integer.BYTES + eggIdSize + passwordSize + metaSize + proteins.filter { it.isPresent }.sumOf {
                it.get().viewType().size + it.get().viewStructure().size
            },
        )
        var offset = copyInt(associatedNest().index(), bytes, 0)
        offset += copyInt(eggIdSize, bytes, offset)
        offset += copyBytes(viewEggId().toByteArray(), bytes, offset, eggIdSize)
        offset += copyInt(passwordSize, bytes, offset)
        offset += copyBytes(viewPassword().toByteArray(), bytes, offset, passwordSize)
        proteins.forEach { slottedProtein ->
            slottedProtein.map { it.viewType().size }.orElse(0).let { typeSize ->
                offset += copyInt(typeSize, bytes, offset)
                if (typeSize > 0) offset += copyBytes(slottedProtein.get().viewType().toByteArray(), bytes, offset, typeSize)
            }
            slottedProtein.map { it.viewStructure().size }.orElse(0).let { structureSize ->
                offset += copyInt(structureSize, bytes, offset)
                if (structureSize > 0) offset += copyBytes(slottedProtein.get().viewStructure().toByteArray(), bytes, offset, structureSize)
            }
        }
        return bytes
    }
}
