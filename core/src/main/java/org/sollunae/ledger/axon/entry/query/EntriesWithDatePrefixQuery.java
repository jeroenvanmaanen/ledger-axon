package org.sollunae.ledger.axon.entry.query;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EntriesWithDatePrefixQuery {
    String datePrefix;
}
