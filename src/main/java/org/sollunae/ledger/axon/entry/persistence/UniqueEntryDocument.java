package org.sollunae.ledger.axon.entry.persistence;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "unique-entry")
@Value
@Builder
public class UniqueEntryDocument {
    @Id
    private String uniqueKey;

    private String aggregateId;
}
