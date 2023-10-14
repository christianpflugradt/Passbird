package de.pflugradts.passbird.domain.service.password.provider;

import de.pflugradts.passbird.domain.model.password.PasswordRequirements;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.CharValue;

import java.security.SecureRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.pflugradts.passbird.domain.model.transfer.CharValueKt.FIRST_DIGIT_INDEX;
import static de.pflugradts.passbird.domain.model.transfer.CharValueKt.LAST_LOWERCASE_INDEX;
import static de.pflugradts.passbird.domain.model.transfer.CharValueKt.MAX_ASCII_VALUE;
import static de.pflugradts.passbird.domain.model.transfer.CharValueKt.MIN_ASCII_VALUE;

public class RandomPasswordProvider implements PasswordProvider {

    private final SecureRandom random = new SecureRandom();

    @Override
    public Bytes createNewPassword(final PasswordRequirements passwordRequirements) {
        Bytes passwordBytes = Bytes.emptyBytes();
        while (!isStrong(passwordBytes, passwordRequirements)) {
            passwordBytes = randomPassword(passwordRequirements);
        }
        return passwordBytes;
    }

    private Bytes randomPassword(final PasswordRequirements passwordRequirements) {
        return Bytes.bytesOf(IntStream.range(0, passwordRequirements.getPasswordLength())
                .mapToObj(i -> nextByte(passwordRequirements)).collect(Collectors.toList()));
    }

    private Byte nextByte(final PasswordRequirements passwordRequirements) {
        return passwordRequirements.getIncludeSpecialCharacters()
                ? randomByte(MIN_ASCII_VALUE, MAX_ASCII_VALUE + 1)
                : randomByte(FIRST_DIGIT_INDEX, LAST_LOWERCASE_INDEX + 1);
    }

    private byte randomByte(final int fromInclusive, final int toExclusive) {
        return (byte) (random.nextInt(toExclusive - fromInclusive) + fromInclusive);
    }

    private boolean isStrong(final Bytes passwordBytes, final PasswordRequirements requirements) {
        return anyMatch(passwordBytes.copy(), c -> CharValue.Companion.charValueOf(c).isDigit())
                && anyMatch(passwordBytes.copy(), c -> CharValue.Companion.charValueOf(c).isUppercaseCharacter())
                && anyMatch(passwordBytes.copy(), c -> CharValue.Companion.charValueOf(c).isLowercaseCharacter())
                && anyMatch(passwordBytes.copy(),
                    c -> CharValue.Companion.charValueOf(c).isSymbol()) == requirements.getIncludeSpecialCharacters();
    }

    private boolean anyMatch(final Bytes bytes, final Predicate<Byte> predicate) {
        final var result = bytes.stream().anyMatch(predicate);
        bytes.scramble();
        return result;
    }

}
