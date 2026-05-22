package de.pflugradts.passbird.domain.model.shell

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell

fun Shell.fakeEnc() = EncryptedShell(iv = emptyShell(), payload = this)
fun EncryptedShell.fakeDec() = payload
