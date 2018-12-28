package org.sollunae.ledger.rest;

import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.compound.command.CreateCompoundCommand;
import org.sollunae.ledger.axon.entry.command.CreateEntryCommand;
import org.sollunae.ledger.axon.entry.command.EntryAddToCompoundCommand;
import org.sollunae.ledger.axon.entry.command.EntryRemoveFromCompoundCommand;
import org.sollunae.ledger.model.EntryData;
import org.sollunae.ledger.model.JarData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

@Component
public class LedgerService implements LedgerApiDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
        try {
            commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(createEntryCommand));
            return ResponseEntity.status(HttpStatus.CREATED).body(id);
        } catch (RuntimeException exception) {
            LOGGER.error("Exception during command execution: {}", exception.getCause(), exception);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    @Override
    public ResponseEntity<String> createCompound() {
        String id = UUID.randomUUID().toString();
        CreateCompoundCommand createCompoundCommand = CreateCompoundCommand.builder()
            .id(id)
            .build();
        try {
            commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(createCompoundCommand));
            return ResponseEntity.status(HttpStatus.CREATED).body(id);
        } catch (RuntimeException exception) {
            LOGGER.error("Exception during command execution: {}", exception.getCause(), exception);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    @Override
    public ResponseEntity<Void> addEntryToCompound(String id, String compoundId) {
        EntryAddToCompoundCommand entryAddToCompoundCommand = EntryAddToCompoundCommand.builder()
            .id(id)
            .compoundId(compoundId)
            .build();
        commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(entryAddToCompoundCommand));
        return ResponseEntity.ok(null);
    }

    @Override
    public ResponseEntity<Void> removeEntryFromCompound(String id) {
        EntryRemoveFromCompoundCommand entryRemoveFromCompoundCommand = EntryRemoveFromCompoundCommand.builder()
            .id(id)
            .build();
        commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(entryRemoveFromCompoundCommand));
        return ResponseEntity.ok(null);
    }
}
