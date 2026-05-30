package de.pflugradts.passbird.adapter.clipboard

import jakarta.inject.Inject
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class ClipboardGateway @Inject constructor() {
    fun copy(text: String) = StringSelection(text).let { Toolkit.getDefaultToolkit().systemClipboard.setContents(it, it) }
}
