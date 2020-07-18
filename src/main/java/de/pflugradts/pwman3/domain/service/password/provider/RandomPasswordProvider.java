package de.pflugradts.pwman3.domain.service.password.provider;

import de.pflugradts.pwman3.application.util.AsciiUtils;
import de.pflugradts.pwman3.domain.model.password.PasswordRequirements;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import static de.pflugradts.pwman3.application.util.AsciiUtils.FIRST_DIGIT_INDEX;
import static de.pflugradts.pwman3.application.util.AsciiUtils.LAST_LOWERCASE_INDEX;
import static de.pflugradts.pwman3.application.util.AsciiUtils.MAX_ASCII_VALUE;
import static de.pflugradts.pwman3.application.util.AsciiUtils.MIN_ASCII_VALUE;

public class RandomPasswordProvider implements PasswordProvider {

    private final SecureRandom random = new SecureRandom();

    @Override
    public Bytes createNewPassword(final PasswordRequirements passwordRequirements) {
        Bytes passwordBytes = Bytes.empty();
        while (!isStrong(passwordBytes, passwordRequirements)) {
            passwordBytes = randomPassword(passwordRequirements);
        }
        return passwordBytes;
    }

    private Bytes randomPassword(final PasswordRequirements passwordRequirements) {
        return Bytes.of(IntStream.range(0, passwordRequirements.getPasswordLength())
                .mapToObj(i -> nextByte(passwordRequirements)).collect(Collectors.toList()));
    }

    private Byte nextByte(final PasswordRequirements passwordRequirements) {
        return passwordRequirements.isIncludeSpecialCharacters()
                ? randomByte(MIN_ASCII_VALUE, MAX_ASCII_VALUE + 1)
                : randomByte(FIRST_DIGIT_INDEX, LAST_LOWERCASE_INDEX + 1);
    }

    private byte randomByte(final int fromInclusive, final int toExclusive) {
        return (byte) (random.nextInt(toExclusive - fromInclusive) + fromInclusive);
    }

    private boolean isStrong(final Bytes passwordBytes, final PasswordRequirements passwordRequirements) {
        var copy = passwordBytes.copy();
        final var hasDigit = StreamSupport.stream(copy.spliterator(), false)
                .anyMatch(AsciiUtils::isDigit);
        copy = copyAndScramble(passwordBytes, copy);
        final var hasUppercase = StreamSupport.stream(passwordBytes.copy().spliterator(), false)
                .anyMatch(AsciiUtils::isUppercaseCharacter);
        copy = copyAndScramble(passwordBytes, copy);
        final var hasLowercase = StreamSupport.stream(passwordBytes.copy().spliterator(), false)
                .anyMatch(AsciiUtils::isLowercaseCharacter);
        copy = copyAndScramble(passwordBytes, copy);
        final var hasSymbol = StreamSupport.stream(passwordBytes.copy().spliterator(), false)
                .anyMatch(AsciiUtils::isSymbol);
        copy.scramble();
        return hasLowercase
                && hasUppercase
                && hasDigit
                && passwordRequirements.isIncludeSpecialCharacters() == hasSymbol;
    }

    private Bytes copyAndScramble(final Bytes original, final Bytes oldCopy) {
        oldCopy.scramble();
        return original.copy();
    }

}
