package org.sollunae.ledger.axon.entry.command;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.sollunae.ledger.axon.LedgerCommand;
import org.sollunae.ledger.axon.once.CascadingCommand;

@Value
@Builder
@Wither
public class EntryUpdateJarCommand implements LedgerCommand, CascadingCommand<EntryUpdateJarCommand> {

    @TargetAggregateIdentifier
    private String id;

    private String sourceAggregateIdentifier;
    private long allocatedToken;
    private String intendedJar;
    private Boolean balanceMatchesIntention;
}
