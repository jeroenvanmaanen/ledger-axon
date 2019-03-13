package org.sollunae.ledger.axon.compound.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.sollunae.ledger.axon.once.WithAllocatedTokens;

import java.util.Map;

@Value
@Builder
@Wither
@JsonDeserialize(builder = CompoundEntryRemovedEvent.CompoundEntryRemovedEventBuilder.class)
public class CompoundEntryRemovedEvent implements WithAllocatedTokens<CompoundEntryRemovedEvent> {
    String id;
    Map<String,Long> allocatedTokens;
    String compoundId;
    String entryId;
}
