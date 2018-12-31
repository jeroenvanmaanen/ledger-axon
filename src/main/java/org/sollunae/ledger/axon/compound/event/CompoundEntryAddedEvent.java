package org.sollunae.ledger.axon.compound.event;

import lombok.Builder;
import lombok.Value;
import org.sollunae.ledger.model.CompoundMemberData;

@Value
@Builder
public class CompoundEntryAddedEvent {
    String compoundId;
    CompoundMemberData member;
}
