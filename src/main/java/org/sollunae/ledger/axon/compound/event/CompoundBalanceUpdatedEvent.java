package org.sollunae.ledger.axon.compound.event;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class CompoundBalanceUpdatedEvent {
    private String compoundId;
    private Map<String,Long> balance;
}
