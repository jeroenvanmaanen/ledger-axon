package org.sollunae.ledger.axon.compound.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.sollunae.ledger.axon.LedgerCommand;
import org.sollunae.ledger.model.CompoundMemberData;

@Value
@Builder
public class CompoundAddEntryCommand implements LedgerCommand {

    @TargetAggregateIdentifier
    private String id;

    private CompoundMemberData member;
    private String currentIntendedJar;
}
