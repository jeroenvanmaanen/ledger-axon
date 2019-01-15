package org.sollunae.ledger.axon.unique.event;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UniqueBucketChildAddedEvent {
    String parentId;
    String childId;
    String keyPrefix;
}
