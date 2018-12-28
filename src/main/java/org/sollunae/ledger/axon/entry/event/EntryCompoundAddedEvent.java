package org.sollunae.ledger.axon.entry.event;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EntryCompoundAddedEvent {
    String entryId;
    String compoundId;
}
