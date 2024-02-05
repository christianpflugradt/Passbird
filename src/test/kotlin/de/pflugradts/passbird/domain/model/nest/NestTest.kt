package de.pflugradts.passbird.domain.model.nest

import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class NestTest {
    @Test
    fun `should create nest`() {
        // given
        val name = "Nest"

        // when
        val actual = createNest(shellOf("Nest"), Slot.DEFAULT)

        // then
        expectThat(actual.viewNestId().asString()) isEqualTo name
    }

    @Test
    fun `should clone shell`() {
        // given
        val shell = shellOf("EggId")
        val nest = createNest(shell, Slot.DEFAULT)

        // when
        shell.scramble()
        val actual = nest.viewNestId()

        // then
        expectThat(actual) isNotEqualTo shell
    }

    @Test
    fun `should create default nest`() {
        // given / when
        val defaultNest = DEFAULT

        // then
        expectThat(defaultNest.viewNestId()) isEqualTo shellOf("Default")
        expectThat(defaultNest.slot) isEqualTo Slot.DEFAULT
    }
}
