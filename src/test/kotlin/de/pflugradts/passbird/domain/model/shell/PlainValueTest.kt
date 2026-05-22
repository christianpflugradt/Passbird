package de.pflugradts.passbird.domain.model.shell

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

private const val DIGITS = "0123456789"
private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
private const val SYMBOLS = "$&[{}(=*)+]!#/@\\';,.-~%`?^|\":<>_ "

class PlainValueTest {
    @Test
    fun `should detect digits`() {
        // given / when / then
        DIGITS.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isDigit).isTrue() }
        DIGITS.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isUppercaseCharacter).isFalse() }
        DIGITS.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isLowercaseCharacter).isFalse() }
        DIGITS.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isAlphabeticCharacter).isFalse() }
        DIGITS.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isSymbol).isFalse() }
    }

    @Test
    fun `should detect uppercase`() {
        // given / when / then
        UPPER.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isDigit).isFalse() }
        UPPER.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isUppercaseCharacter).isTrue() }
        UPPER.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isLowercaseCharacter).isFalse() }
        UPPER.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isAlphabeticCharacter).isTrue() }
        UPPER.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isSymbol).isFalse() }
    }

    @Test
    fun `should detect lowercase`() {
        // given / when / then
        LOWER.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isDigit).isFalse() }
        LOWER.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isUppercaseCharacter).isFalse() }
        LOWER.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isLowercaseCharacter).isTrue() }
        LOWER.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isAlphabeticCharacter).isTrue() }
        LOWER.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isSymbol).isFalse() }
    }

    @Test
    fun `should detect symbols`() {
        // given / when / then
        SYMBOLS.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isDigit).isFalse() }
        SYMBOLS.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isUppercaseCharacter).isFalse() }
        SYMBOLS.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isLowercaseCharacter).isFalse() }
        SYMBOLS.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isAlphabeticCharacter).isFalse() }
        SYMBOLS.toCharArray().forEach { expectThat(PlainValue.plainValueOf(it).isSymbol).isTrue() }
    }
}
