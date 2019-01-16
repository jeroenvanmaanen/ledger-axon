package org.sollunae.ledger.axon.compound.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = CompoundKeyUpdatedEvent.CompoundKeyUpdatedEventBuilder.class)
public class CompoundKeyUpdatedEvent {
    String compoundId;
    String key;
}
