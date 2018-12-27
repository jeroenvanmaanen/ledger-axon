package org.sollunae.ledger.axon.entry.process;

import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.entry.event.EntryCreatedEvent;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
public class EntryEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @EventHandler
    public void on(EntryCreatedEvent entryCreatedEvent) {
        LOGGER.info("Created entry: {}: {}", entryCreatedEvent.getId(), entryCreatedEvent.getData().getDate());
    }
}
