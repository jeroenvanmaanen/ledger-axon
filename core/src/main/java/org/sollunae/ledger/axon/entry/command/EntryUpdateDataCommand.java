package org.sollunae.ledger.axon.entry.command;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.sollunae.ledger.axon.LedgerCommand;
import org.sollunae.ledger.axon.once.CascadingCommand;
import org.sollunae.ledger.model.EntryData;

@Value
@Builder
@Wither
public class EntryUpdateDataCommand implements LedgerCommand, CascadingCommand<EntryUpdateDataCommand> {

    @TargetAggregateIdentifier
    private String id;

    private String sourceAggregateIdentifier;
    private long allocatedToken;
    private EntryData data;
}
