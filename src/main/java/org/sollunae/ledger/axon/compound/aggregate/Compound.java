package org.sollunae.ledger.axon.compound.aggregate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.compound.command.*;
import org.sollunae.ledger.axon.compound.event.*;
import org.sollunae.ledger.model.CompoundMemberData;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@Getter
@NoArgsConstructor
public class Compound {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String id;

    private String key = null;
    private String targetJar = "?";
    private List<String> entryIds = new ArrayList<>();
    private Map<String,CompoundMemberData> members = new HashMap<>();
    private Map<String,Long> balance = new HashMap<>();

    @CommandHandler
    public Compound(CreateCompoundCommand createCompoundCommand) {
        id = createCompoundCommand.getId();
        LOGGER.info("Create compound: {}", id);
        apply(CompoundCreatedEvent.builder().id(id).build());
    }

    @EventSourcingHandler
    public void on(CompoundCreatedEvent compoundCreatedEvent) {
        id = compoundCreatedEvent.getId();
    }

    @CommandHandler
    public void handle(CompoundUpdateKeyCommand command) {
        if (Objects.equals(key, command.getKey())) {
            return;
        }
        apply(CompoundKeyUpdatedEvent.builder().compoundId(command.getId()).key(command.getKey()).build());
    }

    @EventSourcingHandler
    public void on(CompoundKeyUpdatedEvent event) {
        key = event.getKey();
    }

    public void handle(CompoundUpdateTargetJarCommand command) {
        if (Objects.equals(targetJar, command.getTargetJar())) {
            return;
        }
        apply(CompoundTargetJarUpdatedEvent.builder().compoundId(command.getId()).targetJar(command.getTargetJar()).build());
    }

    public void on(CompoundTargetJarUpdatedEvent event) {
        targetJar = event.getTargetJar();
    }

    @CommandHandler
    public void handle(CompoundAddEntryCommand compoundAddEntryCommand) {
        CompoundMemberData member = compoundAddEntryCommand.getMember();
        if (member == null) {
            return;
        }
        String entryId = member.getId();
        members.put(entryId, member);
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
        members.put(entryId, member);
        if (!entryIds.contains(entryId)) {
            entryIds.add(entryId);
        }
    }

    @CommandHandler
    public void handle(CompoundRemoveEntryCommand compoundRemoveEntryCommand) {
        String entryId = compoundRemoveEntryCommand.getEntryId();
        entryIds.remove(entryId);
        if (members.containsKey(entryId)) {
            members.remove(entryId);
            apply(CompoundEntryRemovedEvent.builder().compoundId(id).entryId(entryId).build());
        }
    }

    @EventSourcingHandler
    public void on(CompoundEntryRemovedEvent compoundEntryRemovedEvent) {
        String entryId = compoundEntryRemovedEvent.getEntryId();
        entryIds.remove(entryId);
        members.remove(entryId);
    }

    @CommandHandler
    public void handle(CompoundRebalanceCommand command) {
        Map<String, AtomicLong> counters = new HashMap<>();
        for (CompoundMemberData member : members.values()) {
            String thisJar = member.getJar();
            String contraJar = member.getContraJar();
            addAmount(counters, thisJar, member.getAmountCents());
            addAmount(counters, contraJar, -member.getAmountCents());
        }
        Map<String,Long> newBalance = toBalance(counters);
        if (!newBalance.equals(balance)) {
            apply(CompoundBalanceUpdatedEvent.builder().compoundId(command.getId()).balance(newBalance).build());
        }
    }

    private void addAmount(Map<String,AtomicLong> counters, String jar, Integer amountCents) {
        if (!counters.containsKey(jar)) {
            counters.put(jar, new AtomicLong());
        }
        counters.get(jar).addAndGet(amountCents.longValue());
    }

    private Map<String,Long> toBalance(Map<String,AtomicLong> counters) {
        Map<String,Long> balance = new HashMap<>();
        counters.forEach((key1, value) -> balance.put(key1, value.longValue()));
        return balance;
    }

    public void on(CompoundBalanceUpdatedEvent event) {
        balance = event.getBalance();
    }
}
