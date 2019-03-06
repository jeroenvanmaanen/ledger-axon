package org.sollunae.ledger.once;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.sollunae.ledger.axon.once.WithAllocatedTokens;
import org.springframework.data.util.Pair;

@Value
@Builder
@Wither
public class TestEvent implements WithAllocatedTokens<TestEvent> {
    private Pair<Long,Long> allocatedTokens;
}
