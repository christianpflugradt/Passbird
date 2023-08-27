package de.pflugradts.passbird.domain.model.namespace;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NamespaceSlotTest {

    private final int EXPECTED_DEFAULT_INDEX = 10;

    @Test
    void shouldFindDefaultSlot() {
        // given // when // then
        assertThat(NamespaceSlot.at(EXPECTED_DEFAULT_INDEX)).isNotNull().isEqualTo(NamespaceSlot.DEFAULT);
    }

    @Test
    void shouldFindSlot1() {
        // given // when // then
        assertThat(NamespaceSlot.at(1)).isNotNull().isEqualTo(NamespaceSlot._1);
    }

    @Test
    void shouldFindSlot9() {
        // given // when // then
        assertThat(NamespaceSlot.at(9)).isNotNull().isEqualTo(NamespaceSlot._9);
    }

    @Test
    void shouldReturnDefaultForSlot0() {
        // given // when // then
        assertThat(NamespaceSlot.at(0)).isNotNull().isEqualTo(NamespaceSlot.DEFAULT);
    }

    @Test
    void shouldReturnDefaultForNegativeSlot() {
        // given // when // then
        assertThat(NamespaceSlot.at(-1)).isNotNull().isEqualTo(NamespaceSlot.DEFAULT);
    }

    @Test
    void shouldReturnDefaultForSlotLargerThan10() {
        // given // when // then
        assertThat(NamespaceSlot.at(11)).isNotNull().isEqualTo(NamespaceSlot.DEFAULT);
    }

    @Test
    void shouldReturn10ForDefaultSlot() {
        // given // when // then
        assertThat(NamespaceSlot.DEFAULT.index()).isEqualTo(EXPECTED_DEFAULT_INDEX);
    }

    @Test
    void shouldReturn1ForSlot1() {
        // given // when // then
        assertThat(NamespaceSlot._1.index()).isEqualTo(1);
    }

    @Test
    void shouldReturn9ForSlot9() {
        // given // when // then
        assertThat(NamespaceSlot._9.index()).isEqualTo(9);
    }

}
