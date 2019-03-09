package org.sollunae.ledger.axon;

public interface LedgerCommand {
    String getId();

    default LedgerCommand send(LedgerCommandGateway commandGateway) {
        commandGateway.send(this);
        return this;
    }
}
