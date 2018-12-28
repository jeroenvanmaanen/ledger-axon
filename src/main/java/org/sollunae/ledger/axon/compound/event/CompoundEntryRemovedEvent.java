package org.sollunae.ledger.axon.compound.event;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompoundEntryRemovedEvent {
    String compoundId;
    String entryId;
}
