package org.sollunae.ledger.axon.entry.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = EntryStatusUpdatedEvent.EntryStatusUpdatedEventBuilder.class)
public class EntryStatusUpdatedEvent {
    private String entryId;
    private String intendedJar;
    private Boolean balanceMatchesIntention;
}
