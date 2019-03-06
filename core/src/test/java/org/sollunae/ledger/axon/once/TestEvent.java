package org.sollunae.ledger.axon.once;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Map;

@Value
@Builder
@Wither
public class TestEvent implements WithAllocatedTokens<TestEvent> {
    private String id;
    private Map<String,Long> allocatedTokens;
}
