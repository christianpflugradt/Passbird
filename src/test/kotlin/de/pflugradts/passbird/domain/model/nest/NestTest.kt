package de.pflugradts.passbird.domain.model.nest

import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class NestTest {
    @Test
    fun `should create nest`() {
        // given
        val name = "nest"

        // when
        val actual = createNest(bytesOf("nest"), Slot.DEFAULT)

        // then
        expectThat(actual.bytes.asString()) isEqualTo name
    }

    @Test
    fun `should clone bytes`() {
        // given
        val bytes = bytesOf("eggId")
        val nest = createNest(bytes, Slot.DEFAULT)

        // when
        bytes.scramble()
        val actual = nest.bytes

        // then
        expectThat(actual) isNotEqualTo bytes
    }

    @Test
    fun `should create default nest`() {
        // given / when
        val defaultNest = DEFAULT

        // then
        expectThat(defaultNest.bytes) isEqualTo bytesOf("Default")
        expectThat(defaultNest.slot) isEqualTo Slot.DEFAULT
    }
}
