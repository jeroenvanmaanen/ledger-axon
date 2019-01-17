package org.sollunae.ledger.axon.compound.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.sollunae.ledger.model.CompoundMemberData;

@Value
@Builder
@JsonDeserialize(builder = CompoundEntryAddedEvent.CompoundEntryAddedEventBuilder.class)
public class CompoundEntryAddedEvent {
    String compoundId;
    CompoundMemberData member;
}
