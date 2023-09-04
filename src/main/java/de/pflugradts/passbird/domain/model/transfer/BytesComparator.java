package de.pflugradts.passbird.domain.model.transfer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static de.pflugradts.passbird.domain.model.transfer.CharValue.FIRST_DIGIT_INDEX;
import static de.pflugradts.passbird.domain.model.transfer.CharValue.FIRST_LOWERCASE_INDEX;
import static de.pflugradts.passbird.domain.model.transfer.CharValue.FIRST_UPPERCASE_INDEX;
import static de.pflugradts.passbird.domain.model.transfer.CharValue.LAST_DIGIT_INDEX;
import static de.pflugradts.passbird.domain.model.transfer.CharValue.LAST_LOWERCASE_INDEX;
import static de.pflugradts.passbird.domain.model.transfer.CharValue.LAST_UPPERCASE_INDEX;
import static de.pflugradts.passbird.domain.model.transfer.CharValue.MAX_ASCII_VALUE;
import static de.pflugradts.passbird.domain.model.transfer.CharValue.MIN_ASCII_VALUE;

public class BytesComparator implements Comparator<Bytes> {

    private final Map<Integer, Integer> sortReference = buildSortReference();

    @Override
    public int compare(final Bytes bytes1, final Bytes bytes2) {
        if (Objects.isNull(bytes1)) {
            return compare(Bytes.emptyBytes(), bytes2);
        } else if (Objects.isNull(bytes2)) {
            return compare(bytes1, Bytes.emptyBytes());
        } else if (bytes1.equals(bytes2)) {
            return 0;
        }
        return compareNonEqualBytes(bytes1, bytes2);
    }

    private int compareNonEqualBytes(final Bytes bytes1, final Bytes bytes2) {
        final var reverse = bytes1.getSize() > bytes2.getSize();
        final var b1 = reverse ? bytes2.toByteArray() : bytes1.toByteArray();
        final var b2 = reverse ? bytes1.toByteArray() : bytes2.toByteArray();
        var index = -1;
        var result = b1.length == b2.length ? 0 : -1;
        while (++index < b1.length) {
            final var b1SortIndex = sortReference.get((int) b1[index]);
            final var b2SortIndex = sortReference.get((int) b2[index]);
            if (b1SortIndex.compareTo(b2SortIndex) != 0) {
                result = b1SortIndex < b2SortIndex ? -1 : 1;
                break;
            }
        }
        return reverse ? result * -1 : result;
    }

    private Map<Integer, Integer> buildSortReference() {
        final var sortReferenceMap = new HashMap<Integer, Integer>();
        final var index = new AtomicInteger(0);
        IntStream.range(MIN_ASCII_VALUE, MAX_ASCII_VALUE + 1).filter(i -> CharValue.of(i).isSymbol())
                .forEach(asciiValue -> sortReferenceMap.put(asciiValue, index.getAndIncrement()));
        IntStream.range(FIRST_DIGIT_INDEX, LAST_DIGIT_INDEX + 1)
                .forEach(asciiValue -> sortReferenceMap.put(asciiValue, index.getAndIncrement()));
        final var uppercase = IntStream.range(FIRST_UPPERCASE_INDEX, LAST_UPPERCASE_INDEX + 1)
                .boxed().toList();
        final var lowercase = IntStream.range(FIRST_LOWERCASE_INDEX, LAST_LOWERCASE_INDEX + 1)
                .boxed().toList();
        IntStream.range(0, uppercase.size()).boxed().forEach(i -> {
            sortReferenceMap.put(uppercase.get(i), index.get());
            sortReferenceMap.put(lowercase.get(i), index.getAndIncrement());
        });
        return sortReferenceMap;
    }

}
