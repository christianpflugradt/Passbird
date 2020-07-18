package de.pflugradts.pwman3.application.eventhandling;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.domain.model.ddd.AggregateRoot;
import de.pflugradts.pwman3.domain.model.ddd.DomainEvent;
import de.pflugradts.pwman3.domain.service.eventhandling.EventHandler;
import de.pflugradts.pwman3.domain.service.eventhandling.EventRegistry;
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
public class PwMan3EventRegistry implements EventRegistry {

    @Inject
    private Set<EventHandler> eventHandlers;

    private EventBus eventBus;
    private final Set<AggregateRoot> aggregateRoots = new HashSet<>();
    private final Queue<DomainEvent> domainEvents = new ArrayDeque<>();
    private final Queue<AggregateRoot> abandonedAggregateRoots = new ArrayDeque<>();

    @Override
    public void register(final AggregateRoot aggregateRoot) {
        aggregateRoots.add(aggregateRoot);
    }

    @Override
    public void register(final DomainEvent domainEvent) {
        domainEvents.add(domainEvent);
    }

    @Override
    public void deregister(final AggregateRoot aggregateRoot) {
        abandonedAggregateRoots.add(aggregateRoot);
    }

    @Override
    public void processEvents() {
        processAbandonedAggregateRoots();
        processAggregateRoots();
        processDomainEvents();
        processAbandonedAggregateRoots();
    }

    private void processAggregateRoots() {
        aggregateRoots.forEach(aggregateRoot -> {
            aggregateRoot.getDomainEvents().forEach(domainEvent -> getEventBus().post(domainEvent));
            aggregateRoot.clearDomainEvents();
        });
    }

    private void processDomainEvents() {
        while (!domainEvents.isEmpty()) {
            getEventBus().post(domainEvents.poll());
        }
    }

    private void processAbandonedAggregateRoots() {
        while (!abandonedAggregateRoots.isEmpty()) {
            aggregateRoots.remove(abandonedAggregateRoots.poll());
        }
    }

    private void initializeEventBus() {
        eventBus = new EventBus();
        eventHandlers.forEach(eventBus::register);
    }

    private EventBus getEventBus() {
        if (Objects.isNull(eventBus)) {
            initializeEventBus();
        }
        return eventBus;
    }

}
