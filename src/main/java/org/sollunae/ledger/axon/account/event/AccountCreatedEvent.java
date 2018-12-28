package org.sollunae.ledger.axon.account.event;

import lombok.Builder;
import lombok.Value;
import org.sollunae.ledger.model.AccountData;

@Value
@Builder
public class AccountCreatedEvent {
    private String id;
    private AccountData data;
}
