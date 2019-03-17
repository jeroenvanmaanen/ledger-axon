package org.sollunae.ledger.axon;

import org.axonframework.modelling.command.AggregateLifecycle;
import org.sollunae.ledger.util.AggregateLifecycleBean;

public interface LedgerEvent {

    default void apply() {
        AggregateLifecycle.apply(this);
    }

    default void apply(AggregateLifecycleBean aggregateLifecycle) {
        aggregateLifecycle.apply(this);
    }
}
