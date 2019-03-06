package org.sollunae.ledger.axon.once;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Map;

@Value
@Builder
@Wither
@JsonDeserialize(builder = TokensAllocatedEvent.TokensAllocatedEventBuilder.class)
public class TokensAllocatedEvent implements WithAllocatedTokens<TokensAllocatedEvent> {
    private String id;
    private Map<String,Long> allocatedTokens;
}
