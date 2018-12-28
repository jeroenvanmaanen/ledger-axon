package org.sollunae.ledger.axon.entry.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class EntryRemoveFromCompoundCommand {

    @TargetAggregateIdentifier
    private String id;
}
