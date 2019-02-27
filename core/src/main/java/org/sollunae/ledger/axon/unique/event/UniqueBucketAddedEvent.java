package org.sollunae.ledger.axon.unique.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = UniqueBucketAddedEvent.UniqueBucketAddedEventBuilder.class)
public class UniqueBucketAddedEvent {
    private String id;
    private String fullPrefix;
    private int maxKeys;
    private int childKeyPrefixLength;
}
