package de.pflugradts.pwman3.domain.service;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.domain.model.ddd.AggregateRoot;
import de.pflugradts.pwman3.domain.model.ddd.DomainEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Singleton
public class DomainEventRegistry {

    @Inject
    private DomainEventHandler domainEventHandler;

    private EventBus eventBus;
    private final Set<AggregateRoot> aggregateRoots = new HashSet<>();
    private final Queue<DomainEvent> domainEvents = new ArrayDeque<>();

    public void register(final AggregateRoot aggregateRoot) {
        aggregateRoots.add(aggregateRoot);
    }

    public void register(final DomainEvent domainEvent) {
        domainEvents.add(domainEvent);
    }

    public void processEvents() {
        aggregateRoots.forEach(aggregateRoot -> {
            aggregateRoot.getDomainEvents().forEach(domainEvent -> getEventBus().post(domainEvent));
            aggregateRoot.clearDomainEvents();
        });
        while (!domainEvents.isEmpty()) {
            getEventBus().post(domainEvents.poll());
        }
    }

    private void initializeEventBus() {
        eventBus = new EventBus();
        eventBus.register(domainEventHandler);
    }

    private EventBus getEventBus() {
        if (Objects.isNull(eventBus)) {
            initializeEventBus();
        }
        return eventBus;
    }

}
