package de.pflugradts.passbird.adapter.clipboard
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
class ClipboardGateway constructor() {
    fun copy(text: String) = StringSelection(text).let { Toolkit.getDefaultToolkit().systemClipboard.setContents(it, it) }
}
