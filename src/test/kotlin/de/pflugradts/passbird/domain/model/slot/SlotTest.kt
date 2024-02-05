package de.pflugradts.passbird.domain.model.slot

import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.slot.Slot.S3
import de.pflugradts.passbird.domain.model.slot.Slot.S4
import de.pflugradts.passbird.domain.model.slot.Slot.S5
import de.pflugradts.passbird.domain.model.slot.Slot.S6
import de.pflugradts.passbird.domain.model.slot.Slot.S7
import de.pflugradts.passbird.domain.model.slot.Slot.S8
import de.pflugradts.passbird.domain.model.slot.Slot.S9
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.stream.Stream

class SlotTest {

    @ParameterizedTest
    @MethodSource("providedNestSlotAssignments")
    fun `should resolve nest slot`(slot: Slot, index: Int) {
        expectThat(slotAt(index)) isEqualTo slot
    }

    @ParameterizedTest
    @MethodSource("providedNestSlotAssignments")
    fun `should get index for nest slot`(slot: Slot, index: Int) {
        expectThat(slot.index()) isEqualTo index
    }

    @ParameterizedTest
    @ValueSource(ints = [-9999, -1, 0, 10, 11, 9999])
    fun `should resolve indices less than 1 or greater than 9 to default nest slot`(index: Int) {
        expectThat(slotAt(index)) isEqualTo DEFAULT
    }

    @Test
    fun `should return index 10 for default nest slot`() {
        // given // when // then
        expectThat(DEFAULT.index()) isEqualTo 10
    }

    companion object {
        @JvmStatic
        private fun providedNestSlotAssignments() = Stream.of(
            Arguments.of(S1, 1),
            Arguments.of(S2, 2),
            Arguments.of(S3, 3),
            Arguments.of(S4, 4),
            Arguments.of(S5, 5),
            Arguments.of(S6, 6),
            Arguments.of(S7, 7),
            Arguments.of(S8, 8),
            Arguments.of(S9, 9),
        )
    }
}
