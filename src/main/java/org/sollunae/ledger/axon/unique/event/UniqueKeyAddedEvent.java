package org.sollunae.ledger.axon.unique.event;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UniqueKeyAddedEvent {
    private String bucketId;
    private Object domain;
    private String key;
    private String hash;
}
