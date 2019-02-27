package org.sollunae.ledger.axon.unique.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = FullPrefixAddedEvent.FullPrefixAddedEventBuilder.class)
public class FullPrefixAddedEvent {
    String id;
    String fullPrefix;
}
