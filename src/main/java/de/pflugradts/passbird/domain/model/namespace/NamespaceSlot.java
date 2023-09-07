package de.pflugradts.passbird.domain.model.namespace;

@SuppressWarnings("PMD.FieldNamingConventions")
public enum NamespaceSlot {

    DEFAULT, _1, _2, _3, _4, _5, _6, _7, _8, _9, INVALID;

    public static final int CAPACITY = 9;
    public static final int FIRST_NAMESPACE = 1;
    public static final int LAST_NAMESPACE = 9;
    private static final int DEFAULT_INDEX = 10;

    public static NamespaceSlot at(final char index) {
        try {
            return at(Integer.parseInt(String.valueOf(index)));
        } catch (NumberFormatException ex) {
            return DEFAULT;
        }
    }

    public static NamespaceSlot at(final int index) {
        return index >= FIRST_NAMESPACE && index <= LAST_NAMESPACE ? values()[index] : DEFAULT;
    }

    public int index() {
        for (int index = FIRST_NAMESPACE; index <= LAST_NAMESPACE; index++) {
            if (this.equals(at(index))) {
                return index;
            }
        }
        return DEFAULT_INDEX;
    }

}
