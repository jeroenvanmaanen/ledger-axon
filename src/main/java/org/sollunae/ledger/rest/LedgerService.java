package org.sollunae.ledger.rest;

import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.sollunae.ledger.axon.command.CreateEntryCommand;
import org.sollunae.ledger.model.EntryData;
import org.sollunae.ledger.model.JarData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LedgerService implements LedgerApiDelegate {

    private final CommandGateway commandGateway;

    public LedgerService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @Override
    public ResponseEntity<String> assignEntryToJar(String id, JarData jar) {
        return null;
    }

    @Override
    public ResponseEntity<String> createEntry(EntryData entry) {
        String id = UUID.randomUUID().toString();
        CreateEntryCommand createEntryCommand = CreateEntryCommand.builder()
            .id(id)
            .entry(entry)
            .build();
        commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(createEntryCommand));
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }
}
