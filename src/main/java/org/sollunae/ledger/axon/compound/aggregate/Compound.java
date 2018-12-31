package org.sollunae.ledger.axon.compound.aggregate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.compound.command.CompoundAddEntryCommand;
import org.sollunae.ledger.axon.compound.command.CompoundRemoveEntryCommand;
import org.sollunae.ledger.axon.compound.command.CreateCompoundCommand;
import org.sollunae.ledger.axon.compound.event.CompoundCreatedEvent;
import org.sollunae.ledger.axon.compound.event.CompoundEntryAddedEvent;
import org.sollunae.ledger.axon.compound.event.CompoundEntryRemovedEvent;
import org.sollunae.ledger.model.CompoundMemberData;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@Getter
@NoArgsConstructor
public class Compound {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String id;

    private List<String> entryIds = new ArrayList<>();

    @CommandHandler
    public Compound(CreateCompoundCommand createCompoundCommand) {
        id = UUID.randomUUID().toString();
        LOGGER.info("Create compound: {}", id);
        apply(CompoundCreatedEvent.builder().id(id).build());
    }

    @EventSourcingHandler
    public void on(CompoundCreatedEvent compoundCreatedEvent) {
        id = compoundCreatedEvent.getId();
    }

    @CommandHandler
    public void handle(CompoundAddEntryCommand compoundAddEntryCommand) {
        CompoundMemberData member = compoundAddEntryCommand.getMember();
        if (member == null) {
            return;
        }
        String entryId = member.getId();
        if (!entryIds.contains(entryId)) {
            entryIds.add(entryId);
            apply(CompoundEntryAddedEvent.builder().compoundId(id).member(member).build());
        }
    }

    @EventSourcingHandler
    public void on(CompoundEntryAddedEvent compoundEntryAddedEvent) {
        CompoundMemberData member = compoundEntryAddedEvent.getMember();
        if (member == null) {
            return;
        }
        String entryId = member.getId();
        if (!entryIds.contains(entryId)) {
            entryIds.add(entryId);
        }
    }

    @CommandHandler
    public void handle(CompoundRemoveEntryCommand compoundRemoveEntryCommand) {
        String entryId = compoundRemoveEntryCommand.getEntryId();
        if (entryIds.contains(entryId)) {
            entryIds.remove(entryId);
            apply(CompoundEntryRemovedEvent.builder().compoundId(id).entryId(entryId).build());
        }
    }

    @EventSourcingHandler
    public void on(CompoundEntryRemovedEvent compoundEntryRemovedEvent) {
        String entryId = compoundEntryRemovedEvent.getEntryId();
        entryIds.remove(entryId);
    }
}
