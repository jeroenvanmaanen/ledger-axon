package org.sollunae.ledger.axon.compound.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.sollunae.ledger.model.CompoundMemberData;

@Value
@Builder
public class CompoundAddEntryCommand {

    @TargetAggregateIdentifier
    private String id;

    private CompoundMemberData member;
}
