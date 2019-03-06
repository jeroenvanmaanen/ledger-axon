package org.sollunae.ledger.axon.once;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.axonframework.modelling.command.AggregateIdentifier;

@Value
@Builder
@Wither
public class TestCommand implements CascadingCommand<TestCommand> {

    @AggregateIdentifier
    private String id;

    private String sourceAggregateIdentifier;
    private long allocatedToken;
}
