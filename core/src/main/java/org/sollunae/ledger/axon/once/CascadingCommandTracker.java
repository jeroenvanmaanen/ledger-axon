package org.sollunae.ledger.axon.once;

public interface CascadingCommandTracker {
    CommandCounter getCommandCounter();
}
