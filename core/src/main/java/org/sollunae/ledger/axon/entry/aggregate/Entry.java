package org.sollunae.ledger.axon.entry.aggregate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Aggregate;
import org.sollunae.ledger.axon.LedgerCommand;
import org.sollunae.ledger.axon.LedgerCommandGateway;
import org.sollunae.ledger.axon.account.persistence.AccountDocument;
import org.sollunae.ledger.axon.account.query.AccountByIdQuery;
import org.sollunae.ledger.axon.compound.command.CompoundAddEntryCommand;
import org.sollunae.ledger.axon.compound.command.CompoundRemoveEntryCommand;
import org.sollunae.ledger.axon.entry.command.*;
import org.sollunae.ledger.axon.entry.event.*;
import org.sollunae.ledger.axon.once.CascadingCommandTracker;
import org.sollunae.ledger.axon.once.CommandCounter;
import org.sollunae.ledger.axon.once.TokenFulfilledEvent;
import org.sollunae.ledger.axon.once.TriggerCommandOnceService;
import org.sollunae.ledger.model.AccountData;
import org.sollunae.ledger.model.CompoundMemberData;
import org.sollunae.ledger.model.EntryData;
import org.sollunae.ledger.util.StringUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Slf4j
@Aggregate
@Getter
@NoArgsConstructor
public class Entry implements CascadingCommandTracker {
    private static final String BIJ = "Bij";
    private static final String CREDIT = "Credit";

    @AggregateIdentifier
    private String id;

    private CommandCounter commandCounter;
    private EntryData data;
    private String compoundId;
    private String intendedJar;
    private Boolean balanceMatchesIntention;

    @CommandHandler
    public Entry(CreateEntryCommandUnsafe createCommand, TriggerCommandOnceService onceService, QueryGateway queryGateway) {
        id = createCommand.getId();
        commandCounter = onceService.createCounter();
        log.trace("Create entry: {}: helper: {}", id);
        EntryData data = createCommand.getEntry();
        data.setId(id);
        data.setCompoundId(null);
        data.setIntendedJar(null);
        data.setBalanceMatchesIntention(null);
        Integer amountCents = getAmountCents(data);
        data.setAmountCents(amountCents);
        AccountDocument thisAccount = findAccount(data.getAccount(), queryGateway);
        AccountDocument contraAccount = findAccount(data.getContraAccount(), queryGateway);
        boolean hide = thisAccount != null && contraAccount != null &&  thisAccount.getData().getDepth() > contraAccount.getData().getDepth();
        data.setHidden(hide);
        String thisJar = getJar(thisAccount);
        String contraJar = getJar(contraAccount);
        data.setJar(thisJar);
        data.setContraJar(contraJar);
        this.data = data;
        log.debug("Created entry: {}", data.getKey());
        EntryCreatedEvent.builder()
            .id(id)
            .data(data)
            .build()
            .apply();
        EntryDataUpdatedEvent.builder().id(id).data(data).build().apply();
    }

    private Integer getAmountCents(EntryData entry) {
        int sign = BIJ.equals(entry.getDebetCredit()) || CREDIT.equals(entry.getDebetCredit()) ? +1 : -1;
        return Optional.ofNullable(entry.getAmount())
            .map(s -> s.replaceAll("[^0-9]", ""))
            .filter(StringUtil::isNotEmpty)
            .map(Integer::parseInt)
            .map(cents -> sign * cents)
            .orElse(null)
            ;
    }

    private String getJar(AccountDocument account) {
        return Optional.ofNullable(account)
            .map(AccountDocument::getData)
            .map(AccountData::getKey)
            .orElse("*");
    }

    private AccountDocument findAccount(String accountId, QueryGateway queryGateway) {
        try {
            return queryGateway.query(AccountByIdQuery.builder().id(accountId).build(), AccountDocument.class).get();
        } catch (Exception e) {
            log.trace("Account not found: {}", accountId, e);
            return null;
        }
    }

    @EventSourcingHandler
    public void on(EntryCreatedEvent entryCreatedEvent, TriggerCommandOnceService onceService) {
        if (commandCounter == null) {
            commandCounter = onceService.createCounter();
        }
        id = entryCreatedEvent.getId();
        data = entryCreatedEvent.getData();
    }

    @CommandHandler
    public void handle(EntryAddToCompoundCommand entryAddToCompoundCommand, LedgerCommandGateway commandGateway) {
        String compoundId = entryAddToCompoundCommand.getCompoundId();
        if (compoundId == null) {
            throw new IllegalArgumentException("Compound ID should not be null");
        }
        if (Objects.equals(compoundId, this.compoundId)) {
            return;
        }
        log.trace("On handle entry add to compound command: data: {}", data.toString().replaceAll("[ \t\n]+", " "));
        if (this.compoundId != null) {
            log.debug("Remove entry from previous compound: {}: {}", id, this.compoundId);
            LedgerCommand compoundRemoveEntryCommand = CompoundRemoveEntryCommand.builder()
                .id(this.compoundId).entryId(id).build();
            commandGateway.sendAndWait(compoundRemoveEntryCommand);
        }
        CompoundMemberData member = new CompoundMemberData();
        member.setId(id);
        member.setKey(data.getKey());
        member.setAmountCents(data.getAmountCents());
        member.setJar(data.getJar());
        member.setContraJar(data.getContraJar());
        LedgerCommand compoundAddEntryCommand = CompoundAddEntryCommand.builder()
            .id(compoundId)
            .member(member)
            .currentIntendedJar(intendedJar)
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
    public void handle(EntryRemoveFromCompoundCommand entryRemoveFromCompoundCommand, LedgerCommandGateway commandGateway) {
        if (compoundId == null) {
            return;
        }
        LedgerCommand compoundRemoveEntryCommand = CompoundRemoveEntryCommand.builder()
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
    public void handle(EntryUpdateDataCommand command, TriggerCommandOnceService onceService) {
        if (onceService.isFulfilled(this, command)) {
            return;
        }
        onceService.sendTokenFulfilledEvent(command);
        log.debug("Handle EntryUpdateDataCommand: {}", id);
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
        } else {
            log.debug("Skipped emitting EntryDataUpdatedEvent for: {}", id);
        }
    }

    private <T> boolean differs(Function<EntryData,T> getter, EntryData object, EntryData other) {
        return !Objects.equals(getter.apply(object), getter.apply(other));
    }

    @EventSourcingHandler
    public void on(EntryDataUpdatedEvent event) {
        data = event.getData();
    }

    @EventSourcingHandler
    public void on(TokenFulfilledEvent event, TriggerCommandOnceService onceService) {
        onceService.registerFulfilled(this, event);
    }

    @CommandHandler
    public void handle(EntryUpdateJarCommand command, TriggerCommandOnceService onceService) {
        if (onceService.isFulfilled(this, command)) {
            return;
        }
        onceService.sendTokenFulfilledEvent(command);
        String intendedJar = command.getIntendedJar();
        log.debug("Handle EntryUpdateJarCommand: {}: {} -> {}", id, this.intendedJar, intendedJar);
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
