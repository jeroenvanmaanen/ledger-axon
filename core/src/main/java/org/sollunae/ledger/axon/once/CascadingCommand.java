package org.sollunae.ledger.axon.once;

import java.util.function.Function;

public interface CascadingCommand<T extends CascadingCommand<T>> {
    String getId();
    String getSourceAggregateIdentifier();
    long getAllocatedToken();
    T withAllocatedToken(long token);
    T withSourceAggregateIdentifier(String sourceId);

    @SuppressWarnings("unchecked")
    default T map(Function<T,T> mapper) {
        return mapper.apply((T) this);
    }
}
