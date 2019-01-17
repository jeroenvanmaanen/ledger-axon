package org.sollunae.ledger.axon.account.process;

import org.axonframework.eventhandling.EventHandler;
import org.sollunae.ledger.axon.account.persistence.AccountDocument;
import org.sollunae.ledger.axon.account.event.AccountCreatedEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccountEventHandler {

    @EventHandler
    public void on(AccountCreatedEvent event, MongoTemplate mongoTemplate) {
        try {
            mongoTemplate.insert(AccountDocument.builder().id(event.getId()).data(event.getData()).build());
        } catch (RuntimeException exception) {
            throw new RuntimeException("Could not insert unique key document", exception);
        }
    }
}
