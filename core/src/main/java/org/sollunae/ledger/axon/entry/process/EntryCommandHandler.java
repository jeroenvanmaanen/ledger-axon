package org.sollunae.ledger.axon.entry.process;

import org.axonframework.commandhandling.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.LedgerCommandGateway;
import org.sollunae.ledger.axon.entry.command.CreateEntryCommand;
import org.sollunae.ledger.axon.entry.command.CreateEntryCommandUnsafe;
import org.sollunae.ledger.axon.unique.process.UniqueKeyService;
import org.sollunae.ledger.model.EntryData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntryCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final LedgerCommandGateway ledgerCommandGateway;
    private final UniqueKeyService uniqueKeyService;

    @Autowired
    public EntryCommandHandler(LedgerCommandGateway ledgerCommandGateway, UniqueKeyService uniqueKeyService) {
        this.ledgerCommandGateway = ledgerCommandGateway;
        this.uniqueKeyService = uniqueKeyService;
    }

    @CommandHandler
    public String handle(CreateEntryCommand command) {
        String id = command.getId();
        EntryData data = command.getEntry();
        String key = createUniqueKey(data);
        LOGGER.trace("Create entry: {}: unique key service: {}", id, uniqueKeyService);
        try {
            uniqueKeyService.assertUnique(getClass().toString(), key);
        } catch (IllegalStateException exception) {
            LOGGER.warn("Entry not created; key already exists: {}: {}: {}", key, exception, String.valueOf(exception.getCause()));
            return null;
        }
        return ledgerCommandGateway.sendAndWait(CreateEntryCommandUnsafe.builder().id(id).entry(data).build());
    }

    private String createUniqueKey(EntryData entry) {
        List<String> parts = new ArrayList<>();
        parts.add(DateTimeFormatter.ISO_LOCAL_DATE.format(entry.getDate()));
        parts.add(entry.getAccount());
        parts.add(entry.getAmount());
        parts.add(entry.getDebetCredit());
        parts.add(entry.getCode());
        parts.add(entry.getKind());
        parts.add(entry.getContraAccount());
        parts.add(entry.getDescription());
        parts.add(entry.getRemarks());
        return parts.stream()
            .map(part -> part.replace("%", "%25"))
            .map(part -> part.replace("|", "%7C"))
            .collect(Collectors.joining("|"));
    }
}
