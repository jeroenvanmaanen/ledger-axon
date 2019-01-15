package org.sollunae.ledger.axon.account.persistence;

import lombok.Builder;
import lombok.Value;
import org.sollunae.ledger.model.AccountData;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "account")
@Value
@Builder
public class AccountDocument {

    @Id
    private String id;

    private AccountData data;
}
