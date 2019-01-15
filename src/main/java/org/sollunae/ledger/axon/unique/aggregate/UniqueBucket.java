package org.sollunae.ledger.axon.unique.aggregate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.unique.command.AddUniqueKeyCommand;
import org.sollunae.ledger.axon.unique.command.CreateUniqueBucketCommand;
import org.sollunae.ledger.axon.unique.command.UniqueBucketLogStatisticsCommand;
import org.sollunae.ledger.axon.unique.event.UniqueBucketAddedEvent;
import org.sollunae.ledger.axon.unique.event.UniqueBucketChildAddedEvent;
import org.sollunae.ledger.axon.unique.event.UniqueKeyAddedEvent;
import org.springframework.data.util.Pair;

import java.lang.invoke.MethodHandles;
import java.util.*;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@Getter
@NoArgsConstructor
public class UniqueBucket {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String id;

    private Set<Pair<Object,String>> existingKeys = new HashSet<>();
    private Map<String,String> children = new HashMap<>();
    private int maxKeys;
    private int childKeyPrefixLength;

    @CommandHandler
    public UniqueBucket(CreateUniqueBucketCommand command) {
        id = command.getId();
        apply(UniqueBucketAddedEvent.builder()
            .id(id)
            .maxKeys(command.getMaxKeys())
            .childKeyPrefixLength(command.getChildKeyPrefixLength())
            .build());
    }

    @CommandHandler
    void handle(AddUniqueKeyCommand command, CommandGateway commandGateway) {
        Object domain = command.getDomain();
        String key = command.getKey();
        Pair<Object,String> pair = Pair.of(domain, key);
        if (existingKeys.contains(pair)) {
            throw new IllegalStateException("Key already exists: " + key);
        }
        String hash = command.getHash();
        if (hash.length() <= childKeyPrefixLength || (children.isEmpty() && existingKeys.size() < maxKeys)) {
            existingKeys.add(pair);
            apply(UniqueKeyAddedEvent.builder()
                .bucketId(id)
                .domain(domain)
                .key(key)
                .hash(hash)
                .build());
        } else {
            String childKey = hash.substring(0, childKeyPrefixLength);
            String remainderKey = hash.substring(childKeyPrefixLength);
            String childId = getChildId(childKey, commandGateway);
            commandGateway.sendAndWait(AddUniqueKeyCommand.builder()
                .id(childId)
                .domain(domain)
                .key(key)
                .hash(remainderKey)
                .build());
        }
    }

    private String getChildId(String childKey, CommandGateway commandGateway) {
        if (children.containsKey(childKey)) {
            return children.get(childKey);
        } else {
            String childId = UUID.randomUUID().toString();
            commandGateway.sendAndWait(CreateUniqueBucketCommand.builder()
                .id(childId)
                .maxKeys(maxKeys)
                .childKeyPrefixLength(childKeyPrefixLength)
                .build());
            children.put(childKey, childId);
            apply(UniqueBucketChildAddedEvent.builder().parentId(id).childId(childId).keyPrefix(childKey).build());
            return childId;
        }
    }

    @CommandHandler
    public void handle(UniqueBucketLogStatisticsCommand command) {
        LOGGER.info("Unique bucket: {}: existing keys={}: child buckets={}", id, existingKeys.size(), children.size());
    }

    @EventSourcingHandler
    public void on(UniqueBucketAddedEvent event) {
        id = event.getId();
        maxKeys = event.getMaxKeys();
        childKeyPrefixLength = event.getChildKeyPrefixLength();
    }

    @EventSourcingHandler
    public void on(UniqueKeyAddedEvent event) {
        Pair<Object,String> pair = Pair.of(event.getDomain(), event.getKey());
        existingKeys.add(pair);
    }

    @EventSourcingHandler
    public void on(UniqueBucketChildAddedEvent event) {
        children.put(event.getKeyPrefix(), event.getChildId());
    }
}
