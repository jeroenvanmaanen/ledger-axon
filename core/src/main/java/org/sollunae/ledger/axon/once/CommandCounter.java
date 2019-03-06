package org.sollunae.ledger.axon.once;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Value
@Builder
public class CommandCounter {
    private AtomicLong counter;
    private List<Pair<Long,Long>> state;
}
