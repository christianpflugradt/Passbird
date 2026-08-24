package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.shell.fakeEnc
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isFalse

class EggIdMemoryTest {

    @Test
    fun `should discard matching entry and compact remaining memory`() {
        // given
        val eggIdMemory = EggIdMemory().apply {
            memorize(shellOf("first").fakeEnc(), null)
            memorize(shellOf("second").fakeEnc(), null)
            memorize(shellOf("third").fakeEnc(), null)
        }

        // when
        eggIdMemory.discard(shellOf("second").fakeEnc())

        // then
        expectThat(
            eggIdMemory.take(3).map { it.map { shell -> shell.fakeDec().asString() }.orElse("") },
        ).containsExactly("third", "first", "")
        expectThat(eggIdMemory[2].isPresent).isFalse()
    }
}
