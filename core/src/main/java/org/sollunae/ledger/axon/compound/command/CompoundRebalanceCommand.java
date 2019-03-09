package org.sollunae.ledger.axon.compound.command;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.sollunae.ledger.axon.LedgerCommand;
import org.sollunae.ledger.axon.once.CascadingCommand;

@Value
@Builder
@Wither
public class CompoundRebalanceCommand implements LedgerCommand, CascadingCommand<CompoundRebalanceCommand> {

    @TargetAggregateIdentifier
    private String id;

    private String sourceAggregateIdentifier;
    private long allocatedToken;
    private String addedEntryId;
}
