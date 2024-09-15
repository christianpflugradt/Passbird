package de.pflugradts.passbird.adapter.passwordtree

import com.google.inject.Inject
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.failure.WritePasswordTreeFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.application.util.copyInt
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.FIRST_SLOT
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.LAST_SLOT
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggStreamSupplier
import java.util.Arrays

class PasswordTreeWriter @Inject constructor(
    private val systemOperation: SystemOperation,
    private val configuration: ReadableConfiguration,
    private val nestService: NestService,
    private val cryptoProvider: CryptoProvider,
) {

    fun sync(eggSupplier: EggStreamSupplier) {
        val contentSize = calcRequiredContentSize(eggSupplier)
        val bytes = ByteArray(calcActualTotalSize(contentSize))
        var offset = copyBytes(signature(), bytes, 0, signatureSize())
        for (index in FIRST_SLOT..LAST_SLOT) {
            nestService.atNestSlot(slotAt(index)).asByteArray().let { offset += copyBytes(it, bytes, offset, it.size) }
        }
        eggSupplier.get().forEach { egg -> egg.asByteArray().let { offset += copyBytes(it, bytes, offset, it.size) } }
        val checksumBytes = byteArrayOf(if (contentSize > 0) checksum(Arrays.copyOfRange(bytes, signatureSize(), contentSize)) else 0x0)
        copyBytes(checksumBytes, bytes, offset, checksumBytes())
        writeToDisk(shellOf(bytes))
    }

    private fun writeToDisk(shell: Shell) = tryCatching { systemOperation.writeBytesToFile(filePath, cryptoProvider.encrypt(shell)) }
        .onFailure { reportFailure(WritePasswordTreeFailure(filePath, it)) }

    private fun calcRequiredContentSize(eggs: EggStreamSupplier): Int {
        val eggDataSize = eggs.get()
            .map { egg: Egg ->
                intBytes() + egg.viewEggId().size + egg.viewPassword().size + egg.proteins.filter { it.isPresent }.sumOf {
                    it.get().viewType().size + it.get().viewStructure().size
                }
            }
            .reduce(0) { a: Int, b: Int -> Integer.sum(a, b) }
        val eggMetaSize = eggs.get().count().toInt() * 22 * intBytes()
        val nestSize = Slot.CAPACITY * intBytes() + nestService.all()
            .filter { it.isPresent }
            .map { it.get().viewNestId().size }
            .reduce(0) { a: Int, b: Int -> Integer.sum(a, b) }
        return eggDataSize + eggMetaSize + nestSize
    }

    private val filePath get() =
        systemOperation.resolvePath(configuration.adapter.passwordTree.location.toDirectory(), PASSWORD_TREE_FILENAME.toFileName())
    private fun calcActualTotalSize(contentSize: Int) = signatureSize() + contentSize + checksumBytes()

    private fun Option<Nest>.asByteArray(): ByteArray {
        val nestShell = map { it.viewNestId() }.orElse(emptyShell())
        val nestBytesSize = nestShell.size
        val bytes = ByteArray(Integer.BYTES + nestBytesSize)
        copyInt(nestBytesSize, bytes, 0)
        if (!nestShell.isEmpty) copyBytes(nestShell.toByteArray(), bytes, Integer.BYTES, nestBytesSize)
        return bytes
    }

    private fun Egg.asByteArray(): ByteArray {
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
