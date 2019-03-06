package org.sollunae.ledger.axon.once;

import org.springframework.data.util.Pair;

public interface WithAllocatedTokens<T> {

    Pair<Long,Long> getAllocatedTokens();

    T withAllocatedTokens(Pair<Long,Long> segment);

    default T withAllocatedToken(long token) {
        return withAllocatedTokens(Pair.of(token, token));
    }
}
