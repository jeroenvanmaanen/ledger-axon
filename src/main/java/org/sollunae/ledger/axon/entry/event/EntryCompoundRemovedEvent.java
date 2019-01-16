package org.sollunae.ledger.axon.entry.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = EntryCompoundRemovedEvent.EntryCompoundRemovedEventBuilder.class)
public class EntryCompoundRemovedEvent {
    String entryId;
    String compoundId;
}
