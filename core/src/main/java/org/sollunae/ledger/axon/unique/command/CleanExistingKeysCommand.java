package org.sollunae.ledger.axon.unique.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.sollunae.ledger.axon.LedgerCommand;

@Value
@Builder
public class CleanExistingKeysCommand implements LedgerCommand {

    @TargetAggregateIdentifier
    private String id;

    private String fullPrefix;
}
