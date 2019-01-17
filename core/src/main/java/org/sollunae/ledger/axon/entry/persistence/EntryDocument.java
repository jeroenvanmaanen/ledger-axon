package org.sollunae.ledger.axon.entry.persistence;

import lombok.Builder;
import lombok.Value;
import org.sollunae.ledger.model.EntryData;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "entry")
@Value
@Builder
public class EntryDocument {

    @Id
    private String id;

    private EntryData data;
    private String compoundId;
}
