package org.sollunae.ledger.axon.entry.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.sollunae.ledger.axon.LedgerEvent;
import org.sollunae.ledger.model.EntryData;

@Value
@Builder
@JsonDeserialize(builder = EntryDataUpdatedEvent.EntryDataUpdatedEventBuilder.class)
public class EntryDataUpdatedEvent implements LedgerEvent {
    private String id;
    private EntryData data;
}
