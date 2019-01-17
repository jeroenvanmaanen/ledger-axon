package org.sollunae.ledger.axon.compound.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = CompoundCreatedEvent.CompoundCreatedEventBuilder.class)
public class CompoundCreatedEvent {
    String id;
}
