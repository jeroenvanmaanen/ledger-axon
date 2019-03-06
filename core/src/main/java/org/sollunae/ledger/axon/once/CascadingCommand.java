package org.sollunae.ledger.axon.once;

public interface CascadingCommand {

    String getSourceAggregateIdentifier();
    long getAllocatedToken();
}
