package de.pflugradts.passbird.application.passwordtree

import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.Protein
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test

class PasswordTreePayloadWriterSecretHandlingTest {

    private val cryptoProvider = createAesGcmCipherForTesting()

    @Test
    fun `should scramble temporary egg view shells after writing payload`() {
        val sizingEggId = spyk(cryptoProvider.encrypt(shellOf("egg")))
        val writingEggId = spyk(cryptoProvider.encrypt(shellOf("egg")))
        val sizingPassword = spyk(cryptoProvider.encrypt(shellOf("password")))
        val writingPassword = spyk(cryptoProvider.encrypt(shellOf("password")))
        val egg = mockk<Egg>()
        every { egg.associatedNest() } returns Slot.DEFAULT
        every { egg.viewEggId() } returnsMany listOf(sizingEggId, writingEggId)
        every { egg.viewPassword() } returnsMany listOf(sizingPassword, writingPassword)
        every { egg.proteins } returns List(10) { mutableOptionOf<Protein>() }

        PasswordTreePayloadWriter().write(PasswordTreeSnapshot(eggs = listOf(egg)))

        verify(exactly = 1) { sizingEggId.scramble() }
        verify(exactly = 1) { writingEggId.scramble() }
        verify(exactly = 1) { sizingPassword.scramble() }
        verify(exactly = 1) { writingPassword.scramble() }
    }
}
