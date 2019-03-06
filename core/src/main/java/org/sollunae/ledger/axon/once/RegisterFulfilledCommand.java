package org.sollunae.ledger.axon.once;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.AggregateIdentifier;

@Value
@Builder
public class RegisterFulfilledCommand {

    @AggregateIdentifier
    String id;

    long token;
}
