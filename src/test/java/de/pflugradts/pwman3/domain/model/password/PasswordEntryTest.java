package de.pflugradts.pwman3.domain.model.password;

import de.pflugradts.pwman3.domain.model.event.PasswordEntryCreated;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryDiscarded;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryUpdated;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class PasswordEntryTest {

    @Test
    void shouldViewKey() {
        // given
        final var givenBytes = Bytes.of("myKey");
        final var passwordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenBytes).fake();

        // when
        final var actual = passwordEntry.viewKey();

        // then
        assertThat(actual).isNotNull().isEqualTo(givenBytes);
    }

    @Test
    void shouldViewPassword() {
        // given
        final var givenBytes = Bytes.of("myPassword");
        final var passwordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withPasswordBytes(givenBytes).fake();

        // when
        final var actual = passwordEntry.viewPassword();

        // then
        assertThat(actual).isNotNull().isEqualTo(givenBytes);
    }

    @Test
    void shouldUpdatePassword() {
        // given
        final var givenBytes = Bytes.of("myPassword");
        final var updatedBytes = Bytes.of("newPassword");
        final var passwordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withPasswordBytes(givenBytes).fake();

        // when
        passwordEntry.updatePassword(updatedBytes);
        final var actual = passwordEntry.viewPassword();

        // then
        assertThat(actual).isNotNull()
                .isEqualTo(updatedBytes)
                .isNotEqualTo(givenBytes);
    }

    @Test
    void shouldDiscard() {
        // given
        final var givenBytes = mock(Bytes.class);
        given(givenBytes.copy()).willReturn(givenBytes);

        final var passwordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withPasswordBytes(givenBytes).fake();

        // when
        passwordEntry.discard();

        // then
        then(givenBytes).should().scramble();
    }

    @Nested
    class DomainEventsTest {

        @Test
        void shouldHaveCreatedEvent_WhenPasswordEntryIsCreated() {
            // given / when
            final var passwordEntry = PasswordEntry.create(Bytes.of("key"), Bytes.of("password"));

            // then
            assertThat(passwordEntry).isNotNull()
                    .extracting(PasswordEntry::getDomainEvents).isNotNull()
                    .asList().hasSize(1);
            final var actual = passwordEntry.getDomainEvents().get(0);
            assertThat(actual).isNotNull().isInstanceOf(PasswordEntryCreated.class);
            assertThat((PasswordEntryCreated) actual)
                    .extracting(PasswordEntryCreated::getPasswordEntry)
                    .isNotNull().isEqualTo(passwordEntry);
        }

        @Test
        void shouldHaveUpdatedEvent_PasswordIsUpdated() {
            // given
            final var passwordEntry = PasswordEntryFaker.faker().fakePasswordEntry().fake();

            // when
            passwordEntry.clearDomainEvents();
            passwordEntry.updatePassword(Bytes.of("new password"));

            // then
            assertThat(passwordEntry.getDomainEvents()).isNotNull().asList().hasSize(1);
            final var actual = passwordEntry.getDomainEvents().get(0);
            assertThat(actual).isNotNull().isInstanceOf(PasswordEntryUpdated.class);
            assertThat((PasswordEntryUpdated) actual)
                    .extracting(PasswordEntryUpdated::getPasswordEntry)
                    .isNotNull().isEqualTo(passwordEntry);
        }

        @Test
        void shouldHaveDiscardedEvent_PasswordEntryIsDiscarded() {
            // given
            final var passwordEntry = PasswordEntryFaker.faker().fakePasswordEntry().fake();

            // when
            passwordEntry.clearDomainEvents();
            passwordEntry.discard();

            // then
            assertThat(passwordEntry.getDomainEvents()).isNotNull().asList().hasSize(1);
            final var actual = passwordEntry.getDomainEvents().get(0);
            assertThat(actual).isNotNull().isInstanceOf(PasswordEntryDiscarded.class);
            assertThat((PasswordEntryDiscarded) actual)
                    .extracting(PasswordEntryDiscarded::getPasswordEntry)
                    .isNotNull().isEqualTo(passwordEntry);
        }

    }

}
