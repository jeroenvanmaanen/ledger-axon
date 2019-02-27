package org.sollunae.ledger.axon.unique.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = UniqueBucketChildAddedEvent.UniqueBucketChildAddedEventBuilder.class)
public class UniqueBucketChildAddedEvent {
    String parentId;
    String childId;
    String keyPrefix;
    String fullPrefix;
}
