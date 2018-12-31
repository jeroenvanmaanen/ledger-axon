package org.sollunae.ledger.axon.compound.process;

import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.compound.event.CompoundCreatedEvent;
import org.sollunae.ledger.axon.compound.event.CompoundEntryAddedEvent;
import org.sollunae.ledger.axon.compound.persistence.CompoundDocument;
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

    public void on(CompoundEntryAddedEvent compoundEntryAddedEvent, MongoTemplate mongoTemplate) {
        Query query = Query.query(Criteria.where("id").is(compoundEntryAddedEvent.getCompoundId()));
        Update update = Update.update("memberMap." + compoundEntryAddedEvent.getMember().getId(), compoundEntryAddedEvent.getMember())
            .set("_id", compoundEntryAddedEvent.getCompoundId())
            .set("_class", CompoundDocument.class.getCanonicalName());
        mongoTemplate.upsert(query, update, CompoundDocument.class);
    }
}
