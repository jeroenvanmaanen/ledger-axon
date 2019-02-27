package org.sollunae.ledger.axon.unique.persistence;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "unique-bucket")
@Value
@Builder
public class UniqueBucketDocument {

    @Id
    private String id;

    private String parentId;
    private String keyPrefix;
    private String fullPrefix;
}
