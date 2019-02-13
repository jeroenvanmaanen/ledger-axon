package org.sollunae.ledger.axon;

public interface LedgerCommandGateway {
    String sendAndWait(LedgerCommand ledgerCommand);
    void send(LedgerCommand ledgerCommand);
}
