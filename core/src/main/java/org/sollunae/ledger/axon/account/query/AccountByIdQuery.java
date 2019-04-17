package org.sollunae.ledger.axon.account.query;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountByIdQuery {
    String id;
}
