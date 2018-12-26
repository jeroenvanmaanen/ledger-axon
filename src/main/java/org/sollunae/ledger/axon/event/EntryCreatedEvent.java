package org.sollunae.ledger.axon.event;

import lombok.Builder;
import lombok.Value;
import org.sollunae.ledger.model.EntryData;

@Value
@Builder
public class EntryCreatedEvent {
    String id;
    EntryData data;
}
