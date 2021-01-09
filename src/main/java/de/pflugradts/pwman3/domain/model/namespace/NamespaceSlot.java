package de.pflugradts.pwman3.domain.model.namespace;

@SuppressWarnings("PMD.FieldNamingConventions")
public enum NamespaceSlot {

    DEFAULT, _1, _2, _3, _4, _5, _6, _7, _8, _9;

    public static final int CAPACITY = values().length - 1;
    public static final int FIRST = 1;
    public static final int LAST = values().length - 1;
    private static final int DEFAULT_INDEX = values().length;

    public static NamespaceSlot at(final int index) {
        return index >= FIRST && index <= LAST ? values()[index] : DEFAULT;
    }

    public int index() {
        for (int index = FIRST; index <= LAST; index++) {
            if (this.equals(at(index))) {
                return index;
            }
        }
        return DEFAULT_INDEX;
    }

}
