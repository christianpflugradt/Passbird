package de.pflugradts.pwman3.domain.model.transfer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OutputTest {

    @Nested
    class InstantiationTest {

        @Test
        void shouldHaveBytes() {
            // given
            final var givenBytes = Bytes.of(new byte[]{1, 2, 3});
            final var givenOutput = Output.of(givenBytes);

            // when
            final var actual = givenOutput.getBytes();

            // then
            assertThat(actual).isNotNull().isEqualTo(givenBytes);
        }

        @Test
        void shouldInstantiateFromByteArray() {
            // given
            final var givenByteArray = new byte[]{1, 2, 3};
            final var expectedBytes = Bytes.of(givenByteArray);

            // when
            final var actual = Output.of(givenByteArray);

            // then
            assertThat(actual).isNotNull()
                    .extracting(Output::getBytes)
                    .isNotNull().isEqualTo(expectedBytes);
        }

        @Test
        void shouldInstantiateEmptyOutput() {
            // given / when / then
            assertThat(Output.empty()).isNotNull()
                    .extracting(Output::getBytes)
                    .isNotNull().isEqualTo(Bytes.empty());
        }

    }

}
