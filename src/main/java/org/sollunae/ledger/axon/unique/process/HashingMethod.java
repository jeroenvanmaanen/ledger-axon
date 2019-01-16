package org.sollunae.ledger.axon.unique.process;

import org.springframework.data.util.Pair;

public interface HashingMethod {
    String createHash(Object domain, String key);

    default String createHash(Pair<Object,String> pair) {
        return createHash(pair.getFirst(), pair.getSecond());
    }
}
