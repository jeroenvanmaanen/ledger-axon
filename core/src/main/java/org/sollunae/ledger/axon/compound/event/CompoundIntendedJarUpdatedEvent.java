package org.sollunae.ledger.axon.compound.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.sollunae.ledger.axon.once.WithAllocatedTokens;

import java.util.List;
import java.util.Map;

@Value
@Builder
@Wither
@JsonDeserialize(builder = CompoundIntendedJarUpdatedEvent.CompoundIntendedJarUpdatedEventBuilder.class)
public class CompoundIntendedJarUpdatedEvent implements WithAllocatedTokens<CompoundIntendedJarUpdatedEvent> {
    private String id;
    Map<String,Long> allocatedTokens;
    private String intendedJar;
    private boolean balanceMatchesIntention;
    private List<String> entryIds;
}
