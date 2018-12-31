package org.sollunae.ledger.axon.entry.aggregate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.compound.command.CompoundAddEntryCommand;
import org.sollunae.ledger.axon.compound.command.CompoundRemoveEntryCommand;
import org.sollunae.ledger.axon.entry.command.CreateEntryCommand;
import org.sollunae.ledger.axon.entry.command.EntryAddToCompoundCommand;
import org.sollunae.ledger.axon.entry.command.EntryRemoveFromCompoundCommand;
import org.sollunae.ledger.axon.entry.event.EntryCompoundAddedEvent;
import org.sollunae.ledger.axon.entry.event.EntryCompoundRemovedEvent;
import org.sollunae.ledger.axon.entry.event.EntryCreatedEvent;
import org.sollunae.ledger.axon.entry.persistence.UniqueEntryDocument;
import org.sollunae.ledger.model.CompoundMemberData;
import org.sollunae.ledger.model.EntryData;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.invoke.MethodHandles;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@Getter
@NoArgsConstructor
public class Entry {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String id;

    private EntryData data;
    private String compoundId;

    @CommandHandler
    public Entry(CreateEntryCommand createCommand, MongoTemplate mongoTemplate) {
        id = createCommand.getId();
        LOGGER.trace("Create entry: {}: Mongo template: {}", id, mongoTemplate);
        EntryData data = createCommand.getEntry();
        try {
            mongoTemplate.insert(UniqueEntryDocument.builder().aggregateId(id).uniqueKey(createUniqueKey(data)).build());
        } catch (RuntimeException exception) {
            throw new RuntimeException("Could not insert unique key document", exception);
        }
        data.setId(id);
        apply(EntryCreatedEvent.builder().id(id).data(data).build());
    }

    @EventSourcingHandler
    public void on(EntryCreatedEvent entryCreatedEvent) {
        id = entryCreatedEvent.getId();
        data = entryCreatedEvent.getData();
    }

    @CommandHandler
    public void handle(EntryAddToCompoundCommand entryAddToCompoundCommand, CommandGateway commandGateway) {
        String compoundId = entryAddToCompoundCommand.getCompoundId();
        if (Objects.equals(compoundId, this.compoundId)) {
            return;
        }
        CompoundMemberData member = new CompoundMemberData();
        member.setId(id);
        member.setKey(data.getKey());
        member.setAmountCents(data.getAmountCents());
        member.setJar(data.getJar());
        member.setContraJar(data.getContraJar());
        Object compoundAddEntryCommand = CompoundAddEntryCommand.builder()
            .id(compoundId)
            .member(member)
            .build();
        commandGateway.sendAndWait(compoundAddEntryCommand);
        Object entryCompoundAddedEvent = EntryCompoundAddedEvent.builder()
            .entryId(id)
            .compoundId(compoundId)
            .build();
        apply(entryCompoundAddedEvent);
    }

    @EventSourcingHandler
    public void on(EntryCompoundAddedEvent entryCompoundAddedEvent) {
        this.compoundId = entryCompoundAddedEvent.getCompoundId();
    }

    @CommandHandler
    public void handle(EntryRemoveFromCompoundCommand entryRemoveFromCompoundCommand, CommandGateway commandGateway) {
        if (compoundId == null) {
            return;
        }
        Object compoundRemoveEntryCommand = CompoundRemoveEntryCommand.builder()
            .id(compoundId)
            .entryId(id)
            .build();
        commandGateway.sendAndWait(compoundRemoveEntryCommand);
        Object entryCompoundRemovedEvent = EntryCompoundRemovedEvent.builder()
            .entryId(id)
            .compoundId(compoundId)
            .build();
        apply(entryCompoundRemovedEvent);
    }

    @EventSourcingHandler
    public void on(EntryCompoundRemovedEvent entryCompoundRemovedEvent) {
        this.compoundId = null;
    }

    private String createUniqueKey(EntryData entry) {
        List<String> parts = new ArrayList<>();
        parts.add(DateTimeFormatter.ISO_LOCAL_DATE.format(entry.getDate()));
        parts.add(entry.getAccount());
        parts.add(entry.getAmount());
        parts.add(entry.getDebetCredit());
        parts.add(entry.getCode());
        parts.add(entry.getKind());
        parts.add(entry.getContraAccount());
        parts.add(entry.getDescription());
        parts.add(entry.getRemarks());
        return parts.stream()
            .map(part -> part.replace("%", "%25"))
            .map(part -> part.replace("|", "%7C"))
            .collect(Collectors.joining("|"));
    }
}
