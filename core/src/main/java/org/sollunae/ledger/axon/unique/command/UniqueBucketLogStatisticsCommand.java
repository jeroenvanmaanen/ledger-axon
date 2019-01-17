package org.sollunae.ledger.axon.unique.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class UniqueBucketLogStatisticsCommand {

    @TargetAggregateIdentifier
    private String id;
}
