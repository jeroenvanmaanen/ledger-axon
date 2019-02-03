package org.sollunae.ledger.axon.entry.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class EntryUpdateStatusCommand {

    @TargetAggregateIdentifier
    private String id;

    private Boolean balanceMatchesIntention;
}
