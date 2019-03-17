package org.sollunae.ledger.axon.once;

import org.sollunae.ledger.axon.LedgerEvent;

import java.util.Map;
import java.util.function.Function;

public interface WithAllocatedTokens<T> extends LedgerEvent {

    String getId();
    Map<String,Long> getAllocatedTokens();

    T withAllocatedTokens(Map<String,Long> segment);

    @SuppressWarnings("unchecked")
    default T map(Function<T,T> mapper) {
        return mapper.apply((T) this);
    }
}
