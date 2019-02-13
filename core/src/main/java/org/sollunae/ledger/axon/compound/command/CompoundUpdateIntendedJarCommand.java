package org.sollunae.ledger.axon.compound.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.sollunae.ledger.axon.LedgerCommand;

@Value
@Builder
public class CompoundUpdateIntendedJarCommand implements LedgerCommand {

    @TargetAggregateIdentifier
    private String id;

    private String intendedJar;
}
