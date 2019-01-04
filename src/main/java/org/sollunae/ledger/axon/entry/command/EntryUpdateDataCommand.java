package org.sollunae.ledger.axon.entry.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.sollunae.ledger.model.EntryData;

@Value
@Builder
public class EntryUpdateDataCommand {

    @TargetAggregateIdentifier
    private String id;

    private EntryData data;
}
