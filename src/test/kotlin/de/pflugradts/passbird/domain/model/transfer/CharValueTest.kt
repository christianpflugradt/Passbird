package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.transfer.CharValue.Companion.charValueOf
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

private const val DIGITS = "0123456789"
private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
private const val SYMBOLS = "$&[{}(=*)+]!#/@\\';,.-~%`?^|\":<>_ "

class CharValueTest {
    @Test
    fun `should detect digits`() {
        // given / when / then
        DIGITS.toCharArray().forEach { expectThat(charValueOf(it).isDigit).isTrue() }
        DIGITS.toCharArray().forEach { expectThat(charValueOf(it).isUppercaseCharacter).isFalse() }
        DIGITS.toCharArray().forEach { expectThat(charValueOf(it).isLowercaseCharacter).isFalse() }
        DIGITS.toCharArray().forEach { expectThat(charValueOf(it).isAlphabeticCharacter).isFalse() }
        DIGITS.toCharArray().forEach { expectThat(charValueOf(it).isSymbol).isFalse() }
    }

    @Test
    fun `should detect uppercase`() {
        // given / when / then
        UPPER.toCharArray().forEach { expectThat(charValueOf(it).isDigit).isFalse() }
        UPPER.toCharArray().forEach { expectThat(charValueOf(it).isUppercaseCharacter).isTrue() }
        UPPER.toCharArray().forEach { expectThat(charValueOf(it).isLowercaseCharacter).isFalse() }
        UPPER.toCharArray().forEach { expectThat(charValueOf(it).isAlphabeticCharacter).isTrue() }
        UPPER.toCharArray().forEach { expectThat(charValueOf(it).isSymbol).isFalse() }
    }

    @Test
    fun `should detect lowercase`() {
        // given / when / then
        LOWER.toCharArray().forEach { expectThat(charValueOf(it).isDigit).isFalse() }
        LOWER.toCharArray().forEach { expectThat(charValueOf(it).isUppercaseCharacter).isFalse() }
        LOWER.toCharArray().forEach { expectThat(charValueOf(it).isLowercaseCharacter).isTrue() }
        LOWER.toCharArray().forEach { expectThat(charValueOf(it).isAlphabeticCharacter).isTrue() }
        LOWER.toCharArray().forEach { expectThat(charValueOf(it).isSymbol).isFalse() }
    }

    @Test
    fun `should detect symbols`() {
        // given / when / then
        SYMBOLS.toCharArray().forEach { expectThat(charValueOf(it).isDigit).isFalse() }
        SYMBOLS.toCharArray().forEach { expectThat(charValueOf(it).isUppercaseCharacter).isFalse() }
        SYMBOLS.toCharArray().forEach { expectThat(charValueOf(it).isLowercaseCharacter).isFalse() }
        SYMBOLS.toCharArray().forEach { expectThat(charValueOf(it).isAlphabeticCharacter).isFalse() }
        SYMBOLS.toCharArray().forEach { expectThat(charValueOf(it).isSymbol).isTrue() }
    }
}
