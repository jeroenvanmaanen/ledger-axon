package org.sollunae.ledger.axon.entry.process;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.account.persistence.AccountDocument;
import org.sollunae.ledger.axon.entry.aggregate.Entry;
import org.sollunae.ledger.axon.entry.command.EntryUpdateDataCommand;
import org.sollunae.ledger.axon.entry.event.EntryCompoundAddedEvent;
import org.sollunae.ledger.axon.entry.event.EntryDataUpdatedEvent;
import org.sollunae.ledger.axon.entry.persistence.EntryDocument;
import org.sollunae.ledger.axon.entry.event.EntryCreatedEvent;
import org.sollunae.ledger.model.AccountData;
import org.sollunae.ledger.model.EntryData;
import org.sollunae.ledger.util.StringUtil;
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
    private static final String BIJ = "Bij";
    private static final String CREDIT = "Credit";

    @EventHandler
    public void on(EntryCreatedEvent entryCreatedEvent, MongoTemplate mongoTemplate, CommandGateway commandGateway) {
        EntryData entry = entryCreatedEvent.getData();

        AccountDocument thisAccount = findAccount(entry.getAccount(), mongoTemplate);
        AccountDocument contraAccount = findAccount(entry.getContraAccount(), mongoTemplate);
        boolean hide = thisAccount != null && contraAccount != null &&  thisAccount.getData().getDepth() > contraAccount.getData().getDepth();
        entry.setHidden(hide);

        String thisJar = getJar(thisAccount);
        entry.setJar(thisJar);
        entry.setContraJar(getJar(contraAccount));

        Integer amountCents = getAmountCents(entry);
        entry.setAmountCents(amountCents);

        if (LOGGER.isInfoEnabled()) {
            int cents = Optional.ofNullable(entry.getAmountCents()).orElse(0);
            StringBuilder justCents = new StringBuilder(String.valueOf(Math.abs(cents) % 100));
            while (justCents.length() < 2) {
                justCents.insert(0, "0");
            }
            Object key = Optional.<Object>ofNullable(entry.getKey()).orElse(entry.getDate());
            LOGGER.info("Created entry: {}: {}: {},{}{}", key, thisJar, cents/100, justCents, hide ? " (hidden)" : "");
        }

        commandGateway.send(EntryUpdateDataCommand.builder().id(entryCreatedEvent.getId()).data(entry).build());
    }

    private String getJar(AccountDocument account) {
        return Optional.ofNullable(account)
            .map(AccountDocument::getData)
            .map(AccountData::getKey)
            .orElse("*");
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

    private AccountDocument findAccount(String accountId, MongoTemplate mongoTemplate) {
        Query query = Query.query(Criteria.where("id").is(accountId));
        return mongoTemplate.findOne(query, AccountDocument.class);
    }

    @EventHandler
    public void on(EntryDataUpdatedEvent event, MongoTemplate mongoTemplate) {
        EntryData entry = event.getData();
        Query query = Query.query(Criteria.where("id").is(entry.getId()));
        Update update = Update.update("id", entry.getId()).set("data", entry).set("_class", EntryDocument.class.getCanonicalName());
        mongoTemplate.upsert(query, update, Entry.class);
    }

    @EventHandler
    public void on(EntryCompoundAddedEvent event, MongoTemplate mongoTemplate) {
        String entryId = event.getEntryId();
        String compoundId = event.getCompoundId();
        Query query = Query.query(Criteria.where("id").is(entryId));
        Update update = Update.update("id", entryId).set("compoundId", compoundId).set("_class", EntryDocument.class.getCanonicalName());
        mongoTemplate.upsert(query, update, Entry.class);
    }
}
