package org.sollunae.ledger.axon.unique.aggregate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.sollunae.ledger.axon.LedgerCommandGateway;
import org.sollunae.ledger.axon.unique.command.*;
import org.sollunae.ledger.axon.unique.event.*;
import org.springframework.data.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Slf4j
@Aggregate
@Getter
@NoArgsConstructor
public class UniqueBucket {

    @AggregateIdentifier
    private String id;

    private String fullPrefix;

    private Map<Pair<Object,String>,String> existingKeys = new HashMap<>();
    private Map<String,String> children = new HashMap<>();
    private int maxKeys;
    private int childKeyPrefixLength;

    @CommandHandler
    public UniqueBucket(CreateUniqueBucketCommand command) {
        id = command.getId();
        log.debug("Create unique bucket with ID: {}", id);
        apply(UniqueBucketAddedEvent.builder()
            .id(id)
            .maxKeys(command.getMaxKeys())
            .childKeyPrefixLength(command.getChildKeyPrefixLength())
            .build());
    }

    @CommandHandler
    public String handle(AddUniqueKeyCommand command, LedgerCommandGateway commandGateway) {
        Object domain = command.getDomain();
        String key = command.getKey();
        Pair<Object,String> pair = Pair.of(domain, key);
        if (existingKeys.containsKey(pair)) {
            log.debug("Key already exists: {}: {}", domain, key);
            return null;
        } else if (log.isTraceEnabled()) {
            existingKeys.forEach((p, h) -> traceCompare(pair, p));
        }
        log.debug("Add unique key: existing keys size: {}", existingKeys.size());
        String hash = command.getHash();
        if (hash.length() <= childKeyPrefixLength || (children.isEmpty() && existingKeys.size() < maxKeys)) {
            existingKeys.put(pair, hash);
            log.debug("Key added: {}: {}: {}: {}", id, domain, key, hash);
            apply(UniqueKeyAddedEvent.builder()
                .bucketId(id)
                .domain(domain)
                .key(key)
                .hash(hash)
                .build());
            return key;
        } else {
            String childKey = hash.substring(0, childKeyPrefixLength);
            String remainderKey = hash.substring(childKeyPrefixLength);
            String childId = getChildId(childKey, commandGateway);
            return commandGateway.sendAndWait(AddUniqueKeyCommand.builder()
                .id(childId)
                .domain(domain)
                .key(key)
                .hash(remainderKey)
                .build());
        }
    }

    private <X,Y> void traceCompare(Pair<X,Y> p1, Pair<X,Y> p2) {
        String info = Stream.<Function<Pair<X,Y>,?>>of(Function.identity(), Pair::getFirst, Pair::getSecond)
            .map(f -> {
                Object left = f.apply(p1);
                Object right = f.apply(p2);
                return Stream.of(left,left.hashCode(),right,right.hashCode(),Objects.equals(left,right))
                    .map(String::valueOf)
                    .collect(Collectors.joining(": "));
            })
            .map(x -> "\n" + x)
            .collect(Collectors.joining(": "));
        log.trace("[{} ]", info);
    }

    private String getChildId(String childKey, LedgerCommandGateway commandGateway) {
        if (children.containsKey(childKey)) {
            return children.get(childKey);
        } else {
            String childId = UUID.randomUUID().toString();
            String fullChildPrefix = fullPrefix == null ? null : fullPrefix + childKey;
            commandGateway.sendAndWait(CreateUniqueBucketCommand.builder()
                .id(childId)
                .fullPrefix(fullPrefix)
                .maxKeys(maxKeys)
                .childKeyPrefixLength(childKeyPrefixLength)
                .build());
            children.put(childKey, childId);
            Collection<UniqueKeyRemovedEvent> removedEvents = new ArrayList<>();
            Iterator<Map.Entry<Pair<Object,String>,String>> it = existingKeys.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Pair<Object,String>,String> existingKey = it.next();
                Object domain = existingKey.getKey().getFirst();
                String key = existingKey.getKey().getSecond();
                String hash = existingKey.getValue();
                if (hash.startsWith(childKey)) {
                    transferKeyToChild(domain, key, hash, childKey, childId, removedEvents, commandGateway);
                    it.remove();
                }
            }
            for (UniqueKeyRemovedEvent uniqueKeyRemovedEvent : removedEvents) {
                apply(uniqueKeyRemovedEvent);
            }
            apply(UniqueBucketChildAddedEvent.builder().parentId(id).childId(childId).fullPrefix(fullChildPrefix).keyPrefix(childKey).build());
            return childId;
        }
    }

    private void transferKeyToChild(
        Object domain, String key, String hash, String childKey, String childId,
        Collection<UniqueKeyRemovedEvent> removedEvents, LedgerCommandGateway commandGateway
    ) {
        log.info("Transfer key to child: {}: {}: {}", key, childKey, childId);
        commandGateway.sendAndWait(AddUniqueKeyCommand.builder()
            .id(childId)
            .domain(domain)
            .key(key)
            .hash(hash.substring(childKey.length()))
            .build());
        removedEvents.add(UniqueKeyRemovedEvent.builder()
            .bucketId(id)
            .domain(domain)
            .key(key)
            .hash(hash)
            .build()
        );
    }

    @CommandHandler
    public void handle(CleanExistingKeysCommand command, LedgerCommandGateway commandGateway) {
        String commandFullPrefix = command.getFullPrefix();
        if (fullPrefix == null) {
            fullPrefix = commandFullPrefix;
            log.info("Set full prefix: {}: {}", fullPrefix, id);
            apply(FullPrefixAddedEvent.builder().id(id).fullPrefix(fullPrefix).build());
            for (Map.Entry<String,String> entry : children.entrySet()) {
                String fullChildPrefix = fullPrefix + entry.getKey();
                String childId = entry.getValue();
                commandGateway.sendAndWait(CleanExistingKeysCommand.builder().id(childId).fullPrefix(fullChildPrefix).build());
            }
        } else if (!fullPrefix.equals(commandFullPrefix)) {
            log.error("Cannot change full prefix of existing unique bucket: {}: {}", commandFullPrefix, fullPrefix);
        }

        Collection<UniqueKeyRemovedEvent> removedEvents = new ArrayList<>();
        Iterator<Map.Entry<Pair<Object,String>,String>> it = existingKeys.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Pair<Object,String>,String> existingKey = it.next();
            Object domain = existingKey.getKey().getFirst();
            String key = existingKey.getKey().getSecond();
            String hash = existingKey.getValue();
            String childKey = hash.substring(0, childKeyPrefixLength);
            if (children.containsKey(childKey)) {
                String childId = children.get(childKey);
                transferKeyToChild(domain, key, hash, childKey, childId, removedEvents, commandGateway);
                it.remove();
            }
        }
        for (UniqueKeyRemovedEvent uniqueKeyRemovedEvent : removedEvents) {
            apply(uniqueKeyRemovedEvent);
        }
    }

    @CommandHandler
    public void handle(UniqueBucketLogStatisticsCommand command) {
        log.info("Unique bucket: {}: existing keys={}: child buckets={}", id, existingKeys.size(), children.size());
    }

    @EventSourcingHandler
    public void on(UniqueBucketAddedEvent event) {
        id = event.getId();
        maxKeys = event.getMaxKeys();
        childKeyPrefixLength = event.getChildKeyPrefixLength();
        fullPrefix = event.getFullPrefix();
        log.debug("Event source: {}: {}: {}: {}", UniqueBucketAddedEvent.class, id, maxKeys, childKeyPrefixLength);
    }

    @EventSourcingHandler
    public void on(UniqueKeyAddedEvent event) {
        Pair<Object,String> pair = Pair.of(event.getDomain(), event.getKey());
        String hash = event.getHash();
        existingKeys.put(pair, hash);
        log.debug("Event source: {}: {}: {}: {}", fullPrefix, event.getClass(), toString(pair), hash);
        traceKeys();
    }

    @EventSourcingHandler
    public void on(UniqueKeyRemovedEvent event) {
        Pair<Object,String> pair = Pair.of(event.getDomain(), event.getKey());
        existingKeys.remove(pair);
        log.debug("Event source: {}: {}: {}: {}", fullPrefix, event.getClass(), toString(pair), event.getHash());
        traceKeys();
    }

    private void traceKeys() {
        if (log.isTraceEnabled()) {
            String keys = existingKeys.entrySet().stream()
                .map(e -> String.valueOf(toString(e.getKey()) + " -> " + e.getValue()))
                .collect(Collectors.joining(", "));
            log.trace("Event source: [{}]", keys);
        }
    }

    private String toString(Pair<Object,String> pair) {
        return pair.getFirst() + ":" + pair.getSecond();
    }

    @EventSourcingHandler
    public void on(UniqueBucketChildAddedEvent event) {
        children.put(event.getKeyPrefix(), event.getChildId());
        log.trace("Event source: {}: {}: {}: {}", UniqueBucketChildAddedEvent.class, id, event.getKeyPrefix(), children.size());
        if (log.isTraceEnabled()) {
            String childrenList = children.entrySet().stream()
                .map(e -> String.valueOf(e.getKey() + ":" + e.getValue()))
                .collect(Collectors.joining(", "));
            log.trace("Event source: children list: [{}]",
                UniqueBucketChildAddedEvent.class, id, event.getKeyPrefix(), childrenList);
        }
    }

    @EventSourcingHandler
    public void on(FullPrefixAddedEvent event) {
        fullPrefix = event.getFullPrefix();
    }
}
