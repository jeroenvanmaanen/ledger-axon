package org.sollunae.ledger.axon.account.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.sollunae.ledger.model.AccountData;

@Value
@Builder
public class CreateAccountCommand {

    @TargetAggregateIdentifier
    private String id;

    private AccountData data;
}
