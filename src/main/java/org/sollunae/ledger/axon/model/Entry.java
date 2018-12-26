package org.sollunae.ledger.axon.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.sollunae.ledger.axon.command.CreateEntryCommand;
import org.sollunae.ledger.axon.event.EntryCreatedEvent;
import org.sollunae.ledger.model.EntryData;

import java.util.UUID;

@Aggregate
@Getter
@NoArgsConstructor
public class Entry {

    @AggregateIdentifier
    private String id;

    private EntryData data;

    @CommandHandler
    public Entry(CreateEntryCommand createEntryCommand) {
        id = UUID.randomUUID().toString();
        AggregateLifecycle.apply(EntryCreatedEvent.builder().id(id).data(createEntryCommand.getEntry()).build());
    }

    @EventSourcingHandler
    public void on(EntryCreatedEvent entryCreatedEvent) {
        id = entryCreatedEvent.getId();
        data = entryCreatedEvent.getData();
    }
}
