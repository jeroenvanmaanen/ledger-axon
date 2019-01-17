package org.sollunae.ledger.axon.unique.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class CreateUniqueBucketCommand {

    @TargetAggregateIdentifier
    private String id;

    private int maxKeys;
    private int childKeyPrefixLength;
}
