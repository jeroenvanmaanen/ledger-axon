package org.sollunae.ledger.axon.account.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.sollunae.ledger.model.AccountData;

@Value
@Builder
@JsonDeserialize(builder = AccountCreatedEvent.AccountCreatedEventBuilder.class)
public class AccountCreatedEvent {
    private String id;
    private AccountData data;
}
