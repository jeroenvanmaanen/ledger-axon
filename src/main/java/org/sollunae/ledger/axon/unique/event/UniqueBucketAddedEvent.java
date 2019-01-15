package org.sollunae.ledger.axon.unique.event;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UniqueBucketAddedEvent {
    private String id;
    private int maxKeys;
    private int childKeyPrefixLength;
}
