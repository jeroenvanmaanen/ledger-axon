package org.sollunae.ledger.axon.command;

import lombok.Builder;
import lombok.Value;
import org.sollunae.ledger.model.Entry;

@Value
@Builder
public class CreateEntry {
    private Entry entry;
}
