package org.sollunae.ledger.axon.entry.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = EntryCompoundAddedEvent.EntryCompoundAddedEventBuilder.class)
public class EntryCompoundAddedEvent {
    String entryId;
    String compoundId;
}
