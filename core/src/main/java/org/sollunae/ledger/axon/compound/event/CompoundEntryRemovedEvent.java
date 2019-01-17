package org.sollunae.ledger.axon.compound.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = CompoundEntryRemovedEvent.CompoundEntryRemovedEventBuilder.class)
public class CompoundEntryRemovedEvent {
    String compoundId;
    String entryId;
}
