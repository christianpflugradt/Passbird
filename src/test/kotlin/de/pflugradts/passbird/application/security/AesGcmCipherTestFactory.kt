package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf

fun createAesGcmCipherForTesting() = AesGcmCipher(shellOf("p4s5w0rD!"))
