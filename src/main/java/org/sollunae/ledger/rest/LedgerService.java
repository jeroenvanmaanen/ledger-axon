package org.sollunae.ledger.rest;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.sollunae.ledger.axon.command.CreateEntry;
import org.sollunae.ledger.model.Entry;
import org.sollunae.ledger.model.Jar;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class LedgerService implements LedgerApiDelegate {

    private final CommandBus commandBus;

    public LedgerService(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @Override
    public CompletableFuture<ResponseEntity<String>> assignEntryToJar(String id, Jar jar) {
        return null;
    }

    @Override
    public CompletableFuture<ResponseEntity<String>> createEntry(Entry entry) {
        commandBus.dispatch(GenericCommandMessage.asCommandMessage(CreateEntry.builder().entry(entry).build()));
        return null;
    }
}
