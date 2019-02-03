package org.sollunae.ledger.axon.compound.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = CompoundEntryUpdatedEvent.CompoundEntryUpdatedEventBuilder.class)
public class CompoundEntryUpdatedEvent {
    String compoundId;
    String entryId;
    String intendedJar;
    Boolean balanceMatchesIntention;
}
