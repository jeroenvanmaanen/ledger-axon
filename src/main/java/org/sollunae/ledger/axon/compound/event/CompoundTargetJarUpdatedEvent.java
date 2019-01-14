package org.sollunae.ledger.axon.compound.event;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompoundTargetJarUpdatedEvent {
    private String compoundId;
    private String targetJar;
}
