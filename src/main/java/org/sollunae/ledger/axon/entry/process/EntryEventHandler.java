package org.sollunae.ledger.axon.entry.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.account.aggregate.Account;
import org.sollunae.ledger.axon.entry.aggregate.Entry;
import org.sollunae.ledger.axon.entry.event.EntryCreatedEvent;
import org.sollunae.ledger.model.AccountData;
import org.sollunae.ledger.model.EntryData;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

@Component
public class EntryEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @EventHandler
    public void on(EntryCreatedEvent entryCreatedEvent, MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        EntryData entry = entryCreatedEvent.getData();

        Account thisAccount = findAccount(entry.getAccount(), mongoTemplate);
        Account contraAccount = findAccount(entry.getContraAccount(), mongoTemplate);
        boolean hide = thisAccount != null && contraAccount != null &&  thisAccount.getData().getDepth() > contraAccount.getData().getDepth();
        entry.setHidden(hide);

        String jar = Optional.ofNullable(thisAccount)
            .map(Account::getData)
            .map(AccountData::getKey)
            .orElse("?");
        entry.setJar(jar);

        if (LOGGER.isInfoEnabled()) {
            int cents = Optional.ofNullable(entry.getAmountCents()).orElse(0);
            StringBuilder justCents = new StringBuilder(String.valueOf(cents % 100));
            while (justCents.length() < 2) {
                justCents.insert(0, "0");
            }
            LOGGER.info("Created entry: {}: {}: {},{}{}", entry.getDate(), jar, cents/100, justCents, hide ? " (hidden)" : "");
        }

        Query query = Query.query(Criteria.where("id").is(entry.getId()));
        Update update = Update.update("id", entry.getId()).set("data", entry);
        mongoTemplate.upsert(query, update, Entry.class);
    }

    private Account findAccount(String accountId, MongoTemplate mongoTemplate) {
        Query query = Query.query(Criteria.where("id").is(accountId));
        return mongoTemplate.findOne(query, Account.class);
    }
}
