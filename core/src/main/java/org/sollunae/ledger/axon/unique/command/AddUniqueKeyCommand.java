package org.sollunae.ledger.axon.unique.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.sollunae.ledger.axon.LedgerCommand;

@Value
@Builder
public class AddUniqueKeyCommand implements LedgerCommand {

    @TargetAggregateIdentifier
    private String id;

    private Object domain;
    private String key;
    private String hash;
}
