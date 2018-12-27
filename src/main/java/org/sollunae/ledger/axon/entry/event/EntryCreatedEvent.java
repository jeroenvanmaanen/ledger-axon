package org.sollunae.ledger.axon.entry.event;

import lombok.Builder;
import lombok.Value;
import org.sollunae.ledger.model.EntryData;

@Value
@Builder
public class EntryCreatedEvent {
    String id;
    EntryData data;
}
