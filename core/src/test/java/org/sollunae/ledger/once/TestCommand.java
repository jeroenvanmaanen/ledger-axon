package org.sollunae.ledger.once;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.sollunae.ledger.axon.once.CascadingCommand;

@Value
@Builder
@Wither
public class TestCommand implements CascadingCommand<TestCommand> {

    @AggregateIdentifier
    private String id;

    private String sourceAggregateIdentifier;
    private long allocatedToken;
}
