package org.sollunae.ledger.axon.compound.process;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.compound.command.CompoundRebalanceCommand;
import org.sollunae.ledger.axon.compound.event.*;
import org.sollunae.ledger.axon.compound.persistence.CompoundDocument;
import org.sollunae.ledger.axon.entry.command.EntryUpdateJarCommand;
import org.sollunae.ledger.axon.entry.command.EntryUpdateStatusCommand;
import org.sollunae.ledger.model.CompoundMemberData;
import org.sollunae.ledger.util.StringUtil;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
public class CompoundEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @EventHandler
    public void on(CompoundCreatedEvent compoundCreatedEvent) {
        LOGGER.info("Created compound: {}: {}", compoundCreatedEvent.getId());
    }

    @EventHandler
    public void on(CompoundEntryAddedEvent event, MongoTemplate mongoTemplate, CommandGateway commandGateway) {
        String compoundId = event.getCompoundId();
        CompoundMemberData member = event.getMember();
        LOGGER.info("On compound entry added event: member: {}", member.toString().replaceAll("[ \t\n]+", " "));
        Update update = Update.update("memberMap." + member.getId(), member);
        upsert(update, compoundId, mongoTemplate);
        commandGateway.send(CompoundRebalanceCommand.builder().id(compoundId).build());
    }

    @EventHandler
    public void on(CompoundEntryRemovedEvent event, MongoTemplate mongoTemplate, CommandGateway commandGateway) {
        String compoundId = event.getCompoundId();
        String entryId = event.getEntryId();
        LOGGER.info("On compound entry removed event: member: {}", entryId);
        Query query = Query.query(Criteria.where("id").is(compoundId));
        Update update = new Update();
        update.unset("memberMap." + entryId);
        mongoTemplate.updateFirst(query, update, CompoundDocument.class);
        commandGateway.send(CompoundRebalanceCommand.builder().id(compoundId).build());
    }

    @EventHandler
    public void on(CompoundKeyUpdatedEvent event, MongoTemplate mongoTemplate) {
        String compoundId = event.getCompoundId();
        Update update = Update.update("key", event.getKey());
        upsert(update, compoundId, mongoTemplate);
    }

    @EventHandler
    public void on(CompoundIntendedJarUpdatedEvent event, MongoTemplate mongoTemplate, CommandGateway commandGateway) {
        String compoundId = event.getCompoundId();
        Update update = Update.update("intendedJar", event.getIntendedJar())
            .set("balanceMatchesIntention", event.isBalanceMatchesIntention());
        upsert(update, compoundId, mongoTemplate);
        LOGGER.trace("Send EntryUpdateJarCommands to: {}", StringUtil.asString(event.getEntryIds()));
        for (String entryId : event.getEntryIds()) {
            LOGGER.trace("Send EntryUpdateJarCommand to: {}", entryId);
            commandGateway.sendAndWait(EntryUpdateJarCommand.builder()
                .id(entryId)
                .intendedJar(event.getIntendedJar())
                .balanceMatchesIntention(event.isBalanceMatchesIntention())
                .build()
            );
        }
    }

    @EventHandler
    public void on(CompoundStatusUpdatedEvent event, MongoTemplate mongoTemplate, CommandGateway commandGateway) {
        String compoundId = event.getCompoundId();
        Update update = Update.update("balanceMatchesIntention", event.getBalanceMatchesIntention());
        upsert(update, compoundId, mongoTemplate);
        LOGGER.trace("Send EntryUpdateStatusCommands to: {}", StringUtil.asString(event.getEntryIds()));
        for (String entryId : event.getEntryIds()) {
            LOGGER.trace("Send EntryUpdateStatusCommand to: {}", entryId);
            commandGateway.sendAndWait(EntryUpdateStatusCommand.builder()
                .id(entryId)
                .balanceMatchesIntention(event.getBalanceMatchesIntention())
                .build()
            );
        }
    }

    @EventHandler
    public void on(CompoundBalanceUpdatedEvent event, MongoTemplate mongoTemplate) {
        String compoundId = event.getCompoundId();
        Update update = Update.update("balance", event.getBalance())
            .set("balanceMatchesIntention", event.isBalanceMatchesIntention());
        upsert(update, compoundId, mongoTemplate);
    }

    private void upsert(Update update, String compoundId, MongoTemplate mongoTemplate) {
        Query query = Query.query(Criteria.where("id").is(compoundId));
        update.set("_id", compoundId).set("_class", CompoundDocument.class.getCanonicalName());
        mongoTemplate.upsert(query, update, CompoundDocument.class);
    }
}
