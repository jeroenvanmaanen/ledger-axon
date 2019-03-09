package org.sollunae.ledger.axon.compound.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.sollunae.ledger.axon.once.WithAllocatedTokens;
import org.sollunae.ledger.model.CompoundMemberData;

import java.util.Map;

@Value
@Builder
@Wither
@JsonDeserialize(builder = CompoundEntryAddedEvent.CompoundEntryAddedEventBuilder.class)
public class CompoundEntryAddedEvent implements WithAllocatedTokens<CompoundEntryAddedEvent> {
    String id;
    Map<String,Long> allocatedTokens;
    String compoundId;
    CompoundMemberData member;
}
