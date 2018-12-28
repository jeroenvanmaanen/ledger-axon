package org.sollunae.ledger.axon.compound.process;

import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.compound.event.CompoundCreatedEvent;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
public class CompoundEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @EventHandler
    public void on(CompoundCreatedEvent compoundCreatedEvent) {
        LOGGER.info("Created compound: {}: {}", compoundCreatedEvent.getId());
    }
}
