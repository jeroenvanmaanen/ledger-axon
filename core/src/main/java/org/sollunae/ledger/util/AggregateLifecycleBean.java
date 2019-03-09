package org.sollunae.ledger.util;

import org.axonframework.modelling.command.AggregateLifecycle;
import org.springframework.stereotype.Component;

@Component
public class AggregateLifecycleBean {

    public void apply(Object payload) {
        AggregateLifecycle.apply(payload);
    }
}
