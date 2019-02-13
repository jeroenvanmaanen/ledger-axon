package org.sollunae.ledger.axon;

public interface LedgerCommandGateway {
    String sendAndWait(LedgerCommand ledgerCommand);
}
