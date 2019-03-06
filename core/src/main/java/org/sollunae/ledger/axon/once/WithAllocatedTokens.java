package org.sollunae.ledger.axon.once;

import java.util.Map;

public interface WithAllocatedTokens<T> {

    String getId();
    Map<String,Long> getAllocatedTokens();

    T withAllocatedTokens(Map<String,Long> segment);
}
