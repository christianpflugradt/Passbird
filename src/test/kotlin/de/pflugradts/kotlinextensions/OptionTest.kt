package de.pflugradts.kotlinextensions

import de.pflugradts.kotlinextensions.MutableOption.Companion.emptyOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.kotlinextensions.MutableOption.Companion.optionOf
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNull
import strikt.assertions.isTrue
import java.util.Date
import java.util.stream.Stream

class OptionTest {

    @Nested
    inner class InstantiationTest {
        @Test
        fun `should contain null`() {
            // given / when
            val option = emptyOption<Int>()

            // then
            expectThat(option.isPresent).isFalse()
            expectThat(option.isEmpty).isTrue()
            expectThat(option.orNull()).isNull()
            expectThat(option.orElse(42)) isEqualTo 42
        }

        @Test
        fun `should contain boolean`() {
            // given
            val value = true

            // when
            val option = optionOf(value)

            // then
            expectThat(option.isPresent).isTrue()
            expectThat(option.isEmpty).isFalse()
            expectThat(option.orNull()) isEqualTo value
            expectThat(option.orElse(false)) isEqualTo value
        }

        @Test
        fun `should contain int`() {
            // given
            val value = 42

            // when
            val option = optionOf(value)

            // then
            expectThat(option.isPresent).isTrue()
            expectThat(option.isEmpty).isFalse()
            expectThat(option.orNull()) isEqualTo value
            expectThat(option.orElse(10)) isEqualTo value
        }

        @Test
        fun `should contain string`() {
            // given
            val value = "foo"

            // when
            val option = optionOf(value)

            // then
            expectThat(option.isPresent).isTrue()
            expectThat(option.isEmpty).isFalse()
            expectThat(option.orNull()) isEqualTo value
            expectThat(option.orElse("bar")) isEqualTo value
        }

        @Test
        fun `should contain map`() {
            // given
            val value = mapOf(1 to 'a', 2 to 'b')

            // when
            val option = optionOf(value)

            // then
            expectThat(option.isPresent).isTrue()
            expectThat(option.isEmpty).isFalse()
            expectThat(option.orNull()) isEqualTo value
            expectThat(option.orElse(emptyMap())) isEqualTo value
        }

        @Test
        fun `should contain stream`() {
            // given
            val value = Stream.of(1.1, 2.2, 3.3)

            // when
            val option = optionOf(value)

            // then
            expectThat(option.isPresent).isTrue()
            expectThat(option.isEmpty).isFalse()
            expectThat(option.orNull()) isEqualTo value
            expectThat(option.orElse(Stream.empty())) isEqualTo value
        }

        @Test
        fun `should contain date`() {
            // given
            val value = Date()

            // when
            val option = optionOf(value)

            // then
            expectThat(option.isPresent).isTrue()
            expectThat(option.isEmpty).isFalse()
            expectThat(option.orNull()) isEqualTo value
            expectThat(option.orElse(Date(1L))) isEqualTo value
        }
    }

    @Nested
    inner class MutabilityTest {
        @Test
        fun `should update null to value`() {
            // given
            val initialValue: Int? = null
            val updatedValue = 42
            val option = mutableOptionOf(initialValue)

            // when
            option.set(updatedValue)

            // then
            expectThat(option.orNull()) isNotEqualTo initialValue isEqualTo updatedValue
        }

        @Test
        fun `should update value to null`() {
            // given
            val initialValue = 42
            val updatedValue: Int? = null
            val option = mutableOptionOf(initialValue)

            // when
            option.set(updatedValue)

            // then
            expectThat(option.orNull()) isNotEqualTo initialValue isEqualTo updatedValue
        }

        @Test
        fun `should update value to another value`() {
            // given
            val initialValue = 42
            val updatedValue = 43
            val option = mutableOptionOf(initialValue)

            // when
            option.set(updatedValue)

            // then
            expectThat(option.orNull()) isNotEqualTo initialValue isEqualTo updatedValue
        }
    }

    @Nested
    inner class TransformationTest {

        @Test
        fun `should map value`() {
            // given
            val givenEggId = "EggId1"
            val givenEgg = createEggForTesting(withEggIdShell = shellOf(givenEggId))
            val option = optionOf(givenEgg)

            // when
            val actual = option.map { it.viewEggId().asString() }

            // then
            expectThat(actual.isPresent).isTrue()
            expectThat(actual.get()) isEqualTo givenEggId
        }

        @Test
        fun `should map null value`() {
            // given
            val option = optionOf<Egg>(null)

            // when
            val actual = option.map { it.viewEggId().asString() }

            // then
            expectThat(actual.isPresent).isFalse()
        }

        @Test
        fun `should run block for value in ifPresent`() {
            // given
            var success = false

            // when
            optionOf("not null").ifPresent { success = true }

            // then
            expectThat(success).isTrue()
        }

        @Test
        fun `should not run block for null in ifPresent`() {
            // given
            var success = true

            // when
            optionOf<String>(null).ifPresent { success = false }

            // then
            expectThat(success).isTrue()
        }

        @Test
        fun `should run if block for value in ifPresentOrElse`() {
            // given
            var ifRun = false
            var elseRun = false

            // when
            optionOf("not null").ifPresentOrElse(
                block = { ifRun = true },
                other = { elseRun = true },
            )

            // then
            expectThat(ifRun).isTrue()
            expectThat(elseRun).isFalse()
        }

        @Test
        fun `should run else block for null in ifPresentOrElse`() {
            // given
            var ifRun = false
            var elseRun = false

            // when
            optionOf(null).ifPresentOrElse(
                block = { ifRun = true },
                other = { elseRun = true },
            )

            // then
            expectThat(ifRun).isFalse()
            expectThat(elseRun).isTrue()
        }
    }
}
