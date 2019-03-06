package org.sollunae.ledger.axon.entry.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.sollunae.ledger.axon.once.WithAllocatedTokens;
import org.sollunae.ledger.model.EntryData;

import java.util.Map;

@Value
@Builder
@Wither
@JsonDeserialize(builder = EntryCreatedEvent.EntryCreatedEventBuilder.class)
public class EntryCreatedEvent implements WithAllocatedTokens<EntryCreatedEvent> {
    String id;
    Map<String,Long> allocatedTokens;
    EntryData data;
}
