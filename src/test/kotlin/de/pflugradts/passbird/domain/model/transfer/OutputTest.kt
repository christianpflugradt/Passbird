package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.emptyOutput
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class OutputTest {
    @Nested
    inner class InstantiationTest {
        @Test
        fun `should have shell`() {
            // given
            val givenShell = shellOf(byteArrayOf(1, 2, 3))
            val givenOutput = outputOf(givenShell)

            // when
            val actual = givenOutput.shell

            // then
            expectThat(actual) isEqualTo givenShell
        }

        @Test
        fun `should instantiate from byte array`() {
            // given
            val givenByteArray = byteArrayOf(1, 2, 3)
            val expectedShell = shellOf(givenByteArray)

            // when
            val actual = outputOf(givenByteArray)

            // then
            expectThat(actual.shell) isEqualTo expectedShell
        }

        @Test
        fun `should instantiate empty output`() {
            // given / when / then
            expectThat(emptyOutput().shell) isEqualTo emptyShell()
        }
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val output1 = outputOf(shellOf("abc"))
            val output2 = output1

            // when
            val actual = output1.equals(output2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal to output with equal shell`() {
            // given
            val shell = shellOf("abc")
            val sameShell = shellOf("abc")
            val output1 = outputOf(shell)
            val output2 = outputOf(sameShell)

            // when
            val actual = output1.equals(output2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to output with other shell`() {
            // given
            val shell = shellOf("abc")
            val otherShell = shellOf("abd")
            val output1 = outputOf(shell)
            val output2 = outputOf(otherShell)

            // when
            val actual = output1.equals(output2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val shell = shellOf("abc")
            val output = outputOf(shell)

            // when
            val actual = output.equals(shell)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val output = outputOf(shellOf("abc"))

            // when
            val actual = output.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}
