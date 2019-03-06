package org.sollunae.ledger.axon.once;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Value
@Builder
public class CommandCounter {
    private Map<String,AtomicLong> allocationCounters;
    private Map<String,List<Pair<Long,Long>>> fulfilledState;
}
