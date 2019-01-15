package org.sollunae.ledger.axon.account.aggregate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.account.command.CreateAccountCommand;
import org.sollunae.ledger.axon.account.event.AccountCreatedEvent;
import org.sollunae.ledger.axon.unique.process.UniqueKeyService;
import org.sollunae.ledger.model.AccountData;
import org.springframework.util.StringUtils;

import java.lang.invoke.MethodHandles;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@Getter
@NoArgsConstructor
public class Account {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String id;

    private AccountData data;

    @CommandHandler
    public Account(CreateAccountCommand createCommand, UniqueKeyService uniqueKeyService) {
        AccountData data = createCommand.getData();
        id = data.getAccount();
        String key = data.getKey();
        LOGGER.info("Create account: {}: unique key service: {}", id, uniqueKeyService);
        if (StringUtils.isEmpty(data.getLabel())) {
            data.setLabel(key);
        }
        try {
            uniqueKeyService.assertUnique(getClass(), key);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Entry key already exists: '" + key + "'", exception);
        }
        apply(AccountCreatedEvent.builder().id(id).data(data).build());
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent accountCreatedEvent) {
        data = accountCreatedEvent.getData();
    }
}
