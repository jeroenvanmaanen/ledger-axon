package org.sollunae.ledger.axon.compound.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
@JsonDeserialize(builder = CompoundBalanceUpdatedEvent.CompoundBalanceUpdatedEventBuilder.class)
public class CompoundBalanceUpdatedEvent {
    private String compoundId;
    private Map<String,Long> balance;
    private String affected;
    private boolean balanceMatchesTarget;
}
