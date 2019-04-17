package org.sollunae.ledger.axon.entry.process;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.sollunae.ledger.axon.entry.aggregate.Entry;
import org.sollunae.ledger.axon.entry.event.*;
import org.sollunae.ledger.axon.entry.persistence.EntryDocument;
import org.sollunae.ledger.model.EntryData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class EntryEventHandler {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public EntryEventHandler(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventHandler
    public void on(EntryCreatedEvent entryCreatedEvent) {
        EntryData entry = entryCreatedEvent.getData();

        if (log.isInfoEnabled()) {
            int cents = Optional.ofNullable(entry.getAmountCents()).orElse(0);
            StringBuilder justCents = new StringBuilder(String.valueOf(Math.abs(cents) % 100));
            while (justCents.length() < 2) {
                justCents.insert(0, "0");
            }
            Object key = Optional.<Object>ofNullable(entry.getKey()).orElse(entry.getDate());
            String thisJar = entry.getJar();
            String contraJar = entry.getContraJar();
            boolean hide = entry.isHidden();
            log.info("Created entry: {}: {} -> {}: {},{}{}", key, thisJar, contraJar, cents/100, justCents, hide ? " (hidden)" : "");
        }
    }

    @EventHandler
    public void on(EntryDataUpdatedEvent event) {
        EntryData entry = event.getData();
        if (entry.getIntendedJar() == null) {
            entry.setIntendedJar("?");
            entry.setBalanceMatchesIntention(false);
        }
        log.debug("Entry data updated: {}: {}: {} -> {}: {}", entry.getId(), entry.getDate(), entry.getJar(), entry.getContraJar(), entry.getAmountCents());
        Query query = Query.query(Criteria.where("id").is(entry.getId()));
        Update update = Update.update("id", entry.getId()).set("data", entry).set("_class", EntryDocument.class.getCanonicalName());
        mongoTemplate.upsert(query, update, Entry.class);
    }

    @EventHandler
    public void on(EntryCompoundAddedEvent event) {
        String entryId = event.getEntryId();
        String compoundId = event.getCompoundId();
        Query query = Query.query(Criteria.where("id").is(entryId));
        Update update = Update.update("id", entryId).set("data.compoundId", compoundId).set("_class", EntryDocument.class.getCanonicalName());
        mongoTemplate.upsert(query, update, Entry.class);
    }

    @EventHandler
    public void on(EntryCompoundRemovedEvent event) {
        String entryId = event.getEntryId();
        Query query = Query.query(Criteria.where("id").is(entryId));
        Update update = Update.update("id", entryId).unset("data.compoundId").set("_class", EntryDocument.class.getCanonicalName());
        mongoTemplate.upsert(query, update, Entry.class);
    }

    @EventHandler
    public void on(EntryJarUpdatedEvent event, MongoTemplate mongoTemplate) {
        String entryId = event.getEntryId();
        String intendedJar = event.getIntendedJar();
        Boolean status = event.getBalanceMatchesIntention();
        log.debug("Entry Jar updated: {}: {}: {}", entryId, intendedJar, status);
        Query query = Query.query(Criteria.where("id").is(entryId));
        Update update = Update.update("id", entryId)
            .set("data.intendedJar", intendedJar)
            .set("data.balanceMatchesIntention", status)
            .set("_class", EntryDocument.class.getCanonicalName());
        mongoTemplate.upsert(query, update, Entry.class);
    }

    @EventHandler
    public void on(EntryStatusUpdatedEvent event, MongoTemplate mongoTemplate) {
        String entryId = event.getEntryId();
        String intendedJar = event.getIntendedJar();
        Boolean status = event.getBalanceMatchesIntention();
        log.debug("Entry status updated: {}: {}", entryId, status);
        Query query = Query.query(Criteria.where("id").is(entryId));
        Update update = Update.update("id", entryId)
            .set("data.intendedJar", intendedJar)
            .set("data.balanceMatchesIntention", status)
            .set("_class", EntryDocument.class.getCanonicalName());
        mongoTemplate.upsert(query, update, Entry.class);
    }
}
