package org.sollunae.ledger.axon.unique.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.sollunae.ledger.axon.LedgerCommand;

@Value
@Builder
public class CreateUniqueBucketCommand implements LedgerCommand {

    @TargetAggregateIdentifier
    private String id;

    private int maxKeys;
    private int childKeyPrefixLength;
}
