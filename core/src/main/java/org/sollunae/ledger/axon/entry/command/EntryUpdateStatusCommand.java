package org.sollunae.ledger.axon.entry.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.sollunae.ledger.axon.LedgerCommand;

@Value
@Builder
public class EntryUpdateStatusCommand implements LedgerCommand {

    @TargetAggregateIdentifier
    private String id;

    private String intendedJar;
    private Boolean balanceMatchesIntention;
}
