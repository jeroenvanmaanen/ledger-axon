package org.sollunae.ledger.axon.compound.query;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompoundByIdQuery {
    private String compoundId;
}
