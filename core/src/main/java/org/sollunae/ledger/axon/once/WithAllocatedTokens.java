package org.sollunae.ledger.axon.once;

import org.axonframework.modelling.command.AggregateLifecycle;
import org.sollunae.ledger.util.AggregateLifecycleBean;

import java.util.Map;
import java.util.function.Function;

public interface WithAllocatedTokens<T> {

    String getId();
    Map<String,Long> getAllocatedTokens();

    T withAllocatedTokens(Map<String,Long> segment);

    @SuppressWarnings("unchecked")
    default T map(Function<T,T> mapper) {
        return mapper.apply((T) this);
    }

    default void apply() {
        AggregateLifecycle.apply(this);
    }

    default void apply(AggregateLifecycleBean aggregateLifecycle) {
        aggregateLifecycle.apply(this);
    }
}
