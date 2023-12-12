package de.pflugradts.passbird.domain.model.shell

const val MIN_ASCII_VALUE = 32
const val MAX_ASCII_VALUE = 126
const val FIRST_DIGIT_INDEX = 48
const val LAST_DIGIT_INDEX = 57
const val FIRST_UPPERCASE_INDEX = 65
const val LAST_UPPERCASE_INDEX = 90
const val FIRST_LOWERCASE_INDEX = 97
const val LAST_LOWERCASE_INDEX = 122

class PlainValue private constructor(private val value: Char) {

    val isDigit get() = value.code in FIRST_DIGIT_INDEX..LAST_DIGIT_INDEX
    val isUppercaseCharacter get() = value.code in FIRST_UPPERCASE_INDEX..LAST_UPPERCASE_INDEX
    val isLowercaseCharacter get() = value.code in FIRST_LOWERCASE_INDEX..LAST_LOWERCASE_INDEX
    val isAlphabeticCharacter get() = isUppercaseCharacter || isLowercaseCharacter
    val isSymbol get() = !(isDigit || isAlphabeticCharacter)

    companion object {
        fun plainValueOf(b: Byte) = PlainValue(Char(b.toUShort()))
        fun plainValueOf(i: Int) = PlainValue(i.toChar())
        fun plainValueOf(c: Char) = PlainValue(c)
    }
}
