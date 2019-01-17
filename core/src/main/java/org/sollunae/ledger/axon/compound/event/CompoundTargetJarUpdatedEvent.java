package org.sollunae.ledger.axon.compound.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = CompoundTargetJarUpdatedEvent.CompoundTargetJarUpdatedEventBuilder.class)
public class CompoundTargetJarUpdatedEvent {
    private String compoundId;
    private String targetJar;
    private boolean balanceMatchesTarget;
}
