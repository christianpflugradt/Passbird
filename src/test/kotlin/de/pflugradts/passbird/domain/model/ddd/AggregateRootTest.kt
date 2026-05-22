package de.pflugradts.passbird.domain.model.ddd

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty

class AggregateRootTest {
    @Test
    fun `should register domain event`() {
        // given
        val domainEvent = object : DomainEvent {}
        val aggregate = object : AggregateRoot() {}

        // when
        aggregate.registerDomainEvent(domainEvent)

        // then
        expectThat(aggregate.getDomainEvents()).containsExactly(domainEvent)
    }

    @Test
    fun `should clear domain event`() {
        // given
        val domainEvent = object : DomainEvent {}
        val aggregate = object : AggregateRoot() {}

        // when
        aggregate.registerDomainEvent(domainEvent)
        aggregate.clearDomainEvents()

        // then
        expectThat(aggregate.getDomainEvents()).isEmpty()
    }
}
