package org.sollunae.ledger.axon.compound.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonDeserialize(builder = CompoundIntendedJarUpdatedEvent.CompoundIntendedJarUpdatedEventBuilder.class)
public class CompoundIntendedJarUpdatedEvent {
    private String compoundId;
    private String intendedJar;
    private boolean balanceMatchesIntention;
    private List<String> entryIds;
}
