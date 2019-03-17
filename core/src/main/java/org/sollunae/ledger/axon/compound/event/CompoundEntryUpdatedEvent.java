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
@JsonDeserialize(builder = CompoundEntryUpdatedEvent.CompoundEntryUpdatedEventBuilder.class)
public class CompoundEntryUpdatedEvent implements WithAllocatedTokens<CompoundEntryUpdatedEvent> {
    String id;
    Map<String,Long> allocatedTokens;
    String entryId;
    String intendedJar;
    Boolean balanceMatchesIntention;
}
