package org.sollunae.ledger.axon.compound.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonDeserialize(builder = CompoundStatusUpdatedEvent.CompoundStatusUpdatedEventBuilder.class)
public class CompoundStatusUpdatedEvent {
    private String compoundId;
    private String intendedJar;
    private Boolean balanceMatchesIntention;
    private List<String> entryIds;
}
