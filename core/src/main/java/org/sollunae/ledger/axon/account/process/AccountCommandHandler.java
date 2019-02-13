package org.sollunae.ledger.axon.account.process;

import org.axonframework.commandhandling.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.LedgerCommandGateway;
import org.sollunae.ledger.axon.account.command.CreateAccountCommand;
import org.sollunae.ledger.axon.account.command.CreateAccountCommandUnsafe;
import org.sollunae.ledger.axon.unique.process.UniqueKeyService;
import org.sollunae.ledger.model.AccountData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
public class AccountCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final LedgerCommandGateway ledgerCommandGateway;
    private final UniqueKeyService uniqueKeyService;

    @Autowired
    public AccountCommandHandler(LedgerCommandGateway ledgerCommandGateway, UniqueKeyService uniqueKeyService) {
        this.ledgerCommandGateway = ledgerCommandGateway;
        this.uniqueKeyService = uniqueKeyService;
    }

    @CommandHandler
    public String handle(CreateAccountCommand command) {
        String id = command.getId();
        AccountData data = command.getData();
        String key = data.getKey();
        LOGGER.trace("Create account: {}: unique key service: {}", id, uniqueKeyService);
        try {
            uniqueKeyService.assertUnique(getClass().toString(), key);
        } catch (IllegalStateException exception) {
            LOGGER.warn("Account not created; key already exists: {}: {}: {}", key, exception, String.valueOf(exception.getCause()));
            return null;
        }
        return ledgerCommandGateway.sendAndWait(CreateAccountCommandUnsafe.builder().id(id).data(data).build());
    }
}
