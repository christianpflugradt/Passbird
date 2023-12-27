package de.pflugradts.kotlinextensions

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

class TryResultTest {

    @Test
    fun `should run block successfully`() {
        // given
        val tenDividedBy = { x: Int -> 10 / x }

        // when
        val actual = tryCatching { tenDividedBy(2) }

        // then
        expectThat(actual.success).isTrue()
        expectThat(actual.failure).isFalse()
        expectThat(actual.getOrNull()) isEqualTo 5
        expectThat(actual.getOrElse(0)) isEqualTo 5
        expectThat(actual.exceptionOrNull()).isNull()
    }

    @Test
    fun `should run block returning exception successfully`() {
        // given
        val returnException = { NullPointerException() }

        // when
        val actual = tryCatching { returnException() }

        // then
        expectThat(actual.success).isTrue()
        expectThat(actual.failure).isFalse()
        expectThat(actual.getOrNull()).isA<NullPointerException>()
        expectThat(actual.exceptionOrNull()).isNull()
    }

    @Test
    fun `should capture exception successfully`() {
        // given
        val tenDividedBy = { x: Int -> 10 / x }

        // when
        val actual = tryCatching { tenDividedBy(0) }

        // then
        expectThat(actual.success).isFalse()
        expectThat(actual.failure).isTrue()
        expectThat(actual.getOrNull()).isNull()
        expectThat(actual.getOrElse(-1)) isEqualTo -1
        expectThat(actual.exceptionOrNull()).isA<ArithmeticException>()
    }

    @Test
    fun `should run onSuccess on success`() {
        // given
        val tenDividedBy = { x: Int -> 10 / x }
        val initialState = 0
        val successState = 1
        val failureState = 2
        var state = initialState

        // when
        tryCatching { tenDividedBy(2) }
            .onSuccess { state = successState }
            .onFailure { state = failureState }

        // then
        expectThat(state) isEqualTo successState
    }

    @Test
    fun `should run onFailure on failure`() {
        // given
        val tenDividedBy = { x: Int -> 10 / x }
        val initialState = 0
        val successState = 1
        val failureState = 2
        var state = initialState

        // when
        tryCatching { tenDividedBy(0) }
            .onSuccess { state = successState }
            .onFailure { state = failureState }

        // then
        expectThat(state) isEqualTo failureState
    }

    @Test
    fun `should fold successfully`() {
        // given
        val tenDividedBy = { x: Int -> 10 / x }
        val givenSuccess = tryCatching { tenDividedBy(2) }
        val givenFailure = tryCatching { tenDividedBy(0) }
        val expectedSuccess = "success"
        val expectedFailure = "failure"

        // when
        val actualSuccess = givenSuccess.fold(
            onSuccess = { expectedSuccess },
            onFailure = { expectedFailure },
        )
        val actualFailure = givenFailure.fold(
            onSuccess = { expectedSuccess },
            onFailure = { expectedFailure },
        )

        // then
        expectThat(actualSuccess) isEqualTo expectedSuccess
        expectThat(actualFailure) isEqualTo expectedFailure
    }

    @Test
    fun `should retry successfully`() {
        // given
        var divisor = -1
        val incI = { divisor++ }
        val tenDividedBy = { x: Int -> 10 / x }
        val incAndDivide = {
            tryCatching {
                incI()
                tenDividedBy(divisor)
            }
        }

        // when
        val actual = incAndDivide() // divisor is incremented to 0, result is failure
            .retry { incAndDivide() } // divisor is incremented to 1, result is success
            .retry { incAndDivide() } // divisor is not incremented because result is success
            .retry { incAndDivide() } // divisor is not incremented because result is success

        // then
        expectThat(divisor) isEqualTo 1
        expectThat(actual.success).isTrue()
    }
}
