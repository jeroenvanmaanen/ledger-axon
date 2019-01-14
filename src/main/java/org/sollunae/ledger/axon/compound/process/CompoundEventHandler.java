package org.sollunae.ledger.axon.compound.process;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.compound.command.CompoundRebalanceCommand;
import org.sollunae.ledger.axon.compound.event.CompoundCreatedEvent;
import org.sollunae.ledger.axon.compound.event.CompoundEntryAddedEvent;
import org.sollunae.ledger.axon.compound.event.CompoundKeyUpdatedEvent;
import org.sollunae.ledger.axon.compound.persistence.CompoundDocument;
import org.sollunae.ledger.model.CompoundMemberData;
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
    public void on(CompoundEntryAddedEvent compoundEntryAddedEvent, MongoTemplate mongoTemplate, CommandGateway commandGateway) {
        String compoundId = compoundEntryAddedEvent.getCompoundId();
        CompoundMemberData member = compoundEntryAddedEvent.getMember();
        LOGGER.info("On compound entry added event: member: {}", member.toString().replaceAll("[ \t\n]+", " "));
        Query query = Query.query(Criteria.where("id").is(compoundId));
        Update update = Update.update("memberMap." + member.getId(), member)
            .set("_id", compoundId)
            .set("_class", CompoundDocument.class.getCanonicalName());
        mongoTemplate.upsert(query, update, CompoundDocument.class);
        commandGateway.send(CompoundRebalanceCommand.builder().id(compoundId).build());
    }

    @EventHandler
    public void on(CompoundKeyUpdatedEvent event, MongoTemplate mongoTemplate) {
        Query query = Query.query(Criteria.where("id").is(event.getCompoundId()));
        Update update = Update.update("key", event.getKey());
        mongoTemplate.updateFirst(query, update, CompoundDocument.class);
    }
}
