package de.pflugradts.passbird.application.passwordtree

import de.pflugradts.passbird.domain.model.slot.Slot

internal fun validatePayloadEnvelope(bytes: ByteArray) {
    if (bytes.size < signatureSize() + checksumBytes()) {
        throwUnsupportedPasswordTreeFormat()
    }
}

internal fun payloadContentEnd(bytes: ByteArray) = bytes.size - checksumBytes()

internal fun readPayloadInt(bytes: ByteArray, offset: Int, limitExclusive: Int = bytes.size): Int {
    validatePayloadRange(bytes, offset, Integer.BYTES, limitExclusive)
    return ((bytes[offset].toInt() and 0xFF) shl 24) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
        (bytes[offset + 3].toInt() and 0xFF)
}

internal fun readPayloadSize(bytes: ByteArray, offset: Int, limitExclusive: Int = bytes.size): Int =
    readPayloadInt(bytes, offset, limitExclusive).also {
        if (it < 0) {
            throwUnsupportedPasswordTreeFormat()
        }
    }

internal fun readPayloadBytes(bytes: ByteArray, offset: Int, size: Int, limitExclusive: Int = bytes.size): ByteArray {
    validatePayloadRange(bytes, offset, size, limitExclusive)
    return bytes.copyOfRange(offset, offset + size)
}

internal fun validatePayloadRange(bytes: ByteArray, offset: Int, size: Int, limitExclusive: Int = bytes.size) {
    if (offset < 0 || size < 0) {
        throwUnsupportedPasswordTreeFormat()
    }
    if (limitExclusive < 0 || limitExclusive > bytes.size) {
        throwUnsupportedPasswordTreeFormat()
    }
    if (offset > limitExclusive - size) {
        throwUnsupportedPasswordTreeFormat()
    }
}

internal fun validatePayloadEnd(offset: Int, endExclusive: Int) {
    if (offset != endExclusive) {
        throwUnsupportedPasswordTreeFormat()
    }
}

internal fun storedEggNestSlot(index: Int): Slot {
    if (index != Slot.DEFAULT.index() && index !in Slot.FIRST_SLOT..Slot.LAST_SLOT) {
        throwUnsupportedPasswordTreeFormat()
    }
    return Slot.slotAt(index)
}

internal fun storedTrashFlag(value: Int): Boolean {
    if (value !in 0..1) {
        throwUnsupportedPasswordTreeFormat()
    }
    return value == 1
}

internal fun throwUnsupportedPasswordTreeFormat(): Nothing = throw IllegalStateException("Unsupported password tree format.")
