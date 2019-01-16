package org.sollunae.ledger.axon.compound.persistence;

import lombok.Data;
import org.sollunae.ledger.model.CompoundMemberData;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "compound")
@Data
public class CompoundDocument {

    @Id
    private String id;

    private String key;
    private Map<String, CompoundMemberData> memberMap;
    private Map<String,Long> balance;
    private String targetJar;
    private Boolean balanceMatchesTarget;
}
