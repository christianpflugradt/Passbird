package de.pflugradts.passbird.property

import de.pflugradts.passbird.domain.model.slot.Slot
import io.kotest.property.RandomSource
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import kotlin.random.Random

class PropertyFixturesTest {

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 9, 10])
    fun `should generate unique slot subsets including full capacity without rejection`(size: Int) {
        val samples = slotSubsets(Slot.entries, size..size)
            .samples(RandomSource(Random(42), 42L))
            .take(100)

        samples.forEach { sample ->
            expectThat(sample.value.size) isEqualTo size
            expectThat(sample.value.distinct().size) isEqualTo size
            expectThat(sample.value.all { it in Slot.entries }).isTrue()
            expectThat(sample.value) isEqualTo sample.value.sortedBy(Slot::index)
        }
    }
}
