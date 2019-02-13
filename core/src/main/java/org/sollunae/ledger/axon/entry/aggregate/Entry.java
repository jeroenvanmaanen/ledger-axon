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
import org.sollunae.ledger.axon.entry.command.*;
import org.sollunae.ledger.axon.entry.event.*;
import org.sollunae.ledger.model.CompoundMemberData;
import org.sollunae.ledger.model.EntryData;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.function.Function;

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
    private String intendedJar;
    private Boolean balanceMatchesIntention;

    @CommandHandler
    public Entry(CreateEntryCommandUnsafe createCommand) {
        id = createCommand.getId();
        LOGGER.trace("Create entry: {}: helper: {}", id);
        EntryData data = createCommand.getEntry();
        data.setId(id);
        this.data = data;
        LOGGER.debug("Created entry: {}", data.getKey());
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
        LOGGER.info("On handle entry add to compound command: data: {}", data.toString().replaceAll("[ \t\n]+", " "));
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

    @CommandHandler
    public void handle(EntryUpdateDataCommand command) {
        EntryData commandData = command.getData();
        if (differs(EntryData::getDate, commandData, data) ||
            differs(EntryData::getKey, commandData, data) ||
            differs(EntryData::getAccount, commandData, data) ||
            differs(EntryData::getJar, commandData, data) ||
            differs(EntryData::getAmount, commandData, data) ||
            differs(EntryData::getAmountCents, commandData, data) ||
            differs(EntryData::getDebetCredit, commandData, data) ||
            differs(EntryData::getCode, commandData, data) ||
            differs(EntryData::getKind, commandData, data) ||
            differs(EntryData::getContraAccount, commandData, data) ||
            differs(EntryData::getContraJar, commandData, data) ||
            differs(EntryData::getDescription, commandData, data) ||
            differs(EntryData::getRemarks, commandData, data)
        ) {
            commandData.setId(id);
            apply(EntryDataUpdatedEvent.builder().id(id).data(commandData).build());
        }
    }

    private <T> boolean differs(Function<EntryData,T> getter, EntryData object, EntryData other) {
        return !Objects.equals(getter.apply(object), getter.apply(other));
    }

    @EventSourcingHandler
    public void on(EntryDataUpdatedEvent event) {
        data = event.getData();
    }

    @CommandHandler
    public void handle(EntryUpdateJarCommand command) {
        String intendedJar = command.getIntendedJar();
        Boolean balanceMatchesIntention = command.getBalanceMatchesIntention();
        if (Objects.equals(intendedJar, this.intendedJar) &&
            Objects.equals(balanceMatchesIntention, this.balanceMatchesIntention)
        ) {
            return;
        }
        apply(EntryJarUpdatedEvent.builder()
            .entryId(command.getId())
            .intendedJar(command.getIntendedJar())
            .balanceMatchesIntention(command.getBalanceMatchesIntention())
            .build()
        );
    }

    @EventSourcingHandler
    public void on(EntryJarUpdatedEvent event) {
        intendedJar = event.getIntendedJar();
        balanceMatchesIntention = event.getBalanceMatchesIntention();
    }

    @CommandHandler
    public void handle(EntryUpdateStatusCommand command) {
        Boolean balanceMatchesIntention = command.getBalanceMatchesIntention();
        if (Objects.equals(this.balanceMatchesIntention, balanceMatchesIntention)) {
            return;
        }
        apply(EntryStatusUpdatedEvent.builder()
            .entryId(command.getId())
            .intendedJar(command.getIntendedJar())
            .balanceMatchesIntention(command.getBalanceMatchesIntention())
            .build()
        );
    }

    @EventSourcingHandler
    public void on(EntryStatusUpdatedEvent event) {
        intendedJar = event.getIntendedJar();
        balanceMatchesIntention = event.getBalanceMatchesIntention();
    }
}
