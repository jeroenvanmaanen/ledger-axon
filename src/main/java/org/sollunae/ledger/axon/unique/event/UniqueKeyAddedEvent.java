package org.sollunae.ledger.axon.unique.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = UniqueKeyAddedEvent.UniqueKeyAddedEventBuilder.class)
public class UniqueKeyAddedEvent {
    private String bucketId;
    private Object domain;
    private String key;
    private String hash;
}
