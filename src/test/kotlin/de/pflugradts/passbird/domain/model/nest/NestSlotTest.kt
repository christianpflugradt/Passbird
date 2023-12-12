package de.pflugradts.passbird.domain.model.nest

import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.at
import de.pflugradts.passbird.domain.model.nest.NestSlot.DEFAULT
import de.pflugradts.passbird.domain.model.nest.NestSlot.N1
import de.pflugradts.passbird.domain.model.nest.NestSlot.N2
import de.pflugradts.passbird.domain.model.nest.NestSlot.N3
import de.pflugradts.passbird.domain.model.nest.NestSlot.N4
import de.pflugradts.passbird.domain.model.nest.NestSlot.N5
import de.pflugradts.passbird.domain.model.nest.NestSlot.N6
import de.pflugradts.passbird.domain.model.nest.NestSlot.N7
import de.pflugradts.passbird.domain.model.nest.NestSlot.N8
import de.pflugradts.passbird.domain.model.nest.NestSlot.N9
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.stream.Stream

class NestSlotTest {

    @ParameterizedTest
    @MethodSource("provideNestSlotAssignments")
    fun `should resolve nest slot`(nestSlot: NestSlot, index: Int) {
        expectThat(at(index)) isEqualTo nestSlot
    }

    @ParameterizedTest
    @MethodSource("provideNestSlotAssignments")
    fun `should get index for nest slot`(nestSlot: NestSlot, index: Int) {
        expectThat(nestSlot.index()) isEqualTo index
    }

    @ParameterizedTest
    @ValueSource(ints = [-9999, -1, 0, 10, 11, 9999])
    fun `should resolve indices less than 1 or greater than 9 to default nest slot`(index: Int) {
        expectThat(at(index)) isEqualTo DEFAULT
    }

    @Test
    fun `should return index 10 for default nest slot`() {
        // given // when // then
        expectThat(DEFAULT.index()) isEqualTo 10
    }

    companion object {
        @JvmStatic
        private fun provideNestSlotAssignments() = Stream.of(
            Arguments.of(N1, 1),
            Arguments.of(N2, 2),
            Arguments.of(N3, 3),
            Arguments.of(N4, 4),
            Arguments.of(N5, 5),
            Arguments.of(N6, 6),
            Arguments.of(N7, 7),
            Arguments.of(N8, 8),
            Arguments.of(N9, 9),
        )
    }
}
