package de.pflugradts.passbird.domain.model.password;

import de.pflugradts.passbird.domain.model.PasswordEntryCreated;
import de.pflugradts.passbird.domain.model.PasswordEntryDiscarded;
import de.pflugradts.passbird.domain.model.PasswordEntryUpdated;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class PasswordEntryTest {

    @Test
    void shouldViewKey() {
        // given
        final var givenBytes = Bytes.bytesOf("myKey");
        final var passwordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenBytes).fake();

        // when
        final var actual = passwordEntry.viewKey();

        // then
        assertThat(actual).isNotNull().isEqualTo(givenBytes);
    }

    @Test
    void shouldRenameKey() {
        // given
        final var givenBytes = Bytes.bytesOf("key123");
        final var updatedBytes = Bytes.bytesOf("keyABC");
        final var passwordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(givenBytes).fake();

        // when
        passwordEntry.rename(updatedBytes);
        final var actual = passwordEntry.viewKey();

        // then
        assertThat(actual).isNotNull()
                .isEqualTo(updatedBytes)
                .isNotEqualTo(givenBytes);
    }

    @Test
    void shouldViewPassword() {
        // given
        final var givenBytes = Bytes.bytesOf("myPassword");
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
        final var givenBytes = Bytes.bytesOf("myPassword");
        final var updatedBytes = Bytes.bytesOf("newPassword");
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
    class IdentityTest {

        @Test
        void shouldBeEqualIfKeyAndNamespaceMatch() {
            // given
            final var givenBytes = Bytes.bytesOf("key");
            final var givenNamespace = NamespaceSlot.N1;

            final var passwordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withNamespace(givenNamespace)
                .withKeyBytes(givenBytes).fake();

            final var passwordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withNamespace(givenNamespace)
                .withKeyBytes(givenBytes).fake();

            // when
            final var actual = passwordEntry1.equals(passwordEntry2);

            // then
            assertThat(actual).isTrue();
        }

        @Test
        void shouldNotBeEqualIfKeyDoesNotMatch() {
            // given
            final var givenBytes = Bytes.bytesOf("key");
            final var otherBytes = Bytes.bytesOf("key2");
            final var givenNamespace = NamespaceSlot.N1;

            final var passwordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withNamespace(givenNamespace)
                .withKeyBytes(givenBytes).fake();

            final var passwordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withNamespace(givenNamespace)
                .withKeyBytes(otherBytes).fake();

            // when
            final var actual = passwordEntry1.equals(passwordEntry2);

            // then
            assertThat(actual).isFalse();
        }

        @Test
        void shouldNotBeEqualIfNamespaceDoesNotMatch() {
            // given
            final var givenBytes = Bytes.bytesOf("key");
            final var givenNamespace = NamespaceSlot.N1;
            final var otherNamespace = NamespaceSlot.N2;

            final var passwordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withNamespace(givenNamespace)
                .withKeyBytes(givenBytes).fake();

            final var passwordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withNamespace(otherNamespace)
                .withKeyBytes(givenBytes).fake();

            // when
            final var actual = passwordEntry1.equals(passwordEntry2);

            // then
            assertThat(actual).isFalse();
        }

    }


    @Nested
    class DomainEventsTest {

        @Test
        void shouldHaveCreatedEvent_WhenPasswordEntryIsCreated() {
            // given / when
            final var passwordEntry = PasswordEntry.create(DEFAULT, Bytes.bytesOf("key"), Bytes.bytesOf("password"));

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
            passwordEntry.updatePassword(Bytes.bytesOf("new password"));

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
