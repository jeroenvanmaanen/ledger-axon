package org.sollunae.ledger.axon.compound.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class CompoundRemoveEntryCommand {

    @TargetAggregateIdentifier
    private String id;

    private String entryId;
}
