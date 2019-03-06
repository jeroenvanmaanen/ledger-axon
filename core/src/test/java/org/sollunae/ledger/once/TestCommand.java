package org.sollunae.ledger.once;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.sollunae.ledger.axon.once.CascadingCommand;

@Value
@Builder
public class TestCommand implements CascadingCommand {

    @AggregateIdentifier
    String id;

    private String sourceAggregateIdentifier;
    private long allocatedToken;
}
