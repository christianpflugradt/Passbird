package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf

fun createAesGcmCipherForTesting() = AesGcmCipher(createTestKeyShell())
fun createLegacyAesGcmCipherForTesting() = createLegacyAesGcmCipher(createTestKeyShell())
fun createTestKeyShell() = shellOf("p4s5w0rD!1234567")
