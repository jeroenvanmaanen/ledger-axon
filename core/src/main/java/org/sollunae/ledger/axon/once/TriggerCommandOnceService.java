package org.sollunae.ledger.axon.once;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Component
public class TriggerCommandOnceService {

    public CommandCounter createCounter() {
        return CommandCounter.builder()
            .allocationCounters(new HashMap<>())
            .fulfilledState(new HashMap<>())
            .build();
    }

    public <T extends WithAllocatedTokens<T>> T allocate(CascadingCommandTracker tracker, T event, String... targetIds) {
        if (targetIds == null || targetIds.length < 1) {
            return event;
        }
        Map<String,Long> tokens = new HashMap<>();
        for (String targetId : targetIds) {
            long token = allocate(tracker.getCommandCounter(), targetId);
            tokens.put(targetId, token);
        }
        return event.withAllocatedTokens(tokens);
    }

    public long allocate(CommandCounter commandCounter, String targetId) {
        return getAllocationCounter(commandCounter, targetId).incrementAndGet();
    }

    private AtomicLong getAllocationCounter(CommandCounter commandCounter, String targetId) {
        return commandCounter.getAllocationCounters().computeIfAbsent(targetId, k -> new AtomicLong());
    }

    public <T extends CascadingCommand<T>> Function<T,T> prepareCommand(WithAllocatedTokens<?> event) {
        return command -> prepareCommand(command, event);
    }

    private <T extends CascadingCommand<T>> T prepareCommand(T command, WithAllocatedTokens<?> event) {
        return command
            .withSourceAggregateIdentifier(event.getId())
            .withAllocatedToken(event.getAllocatedTokens().get(command.getId()));
    }

    public boolean checkIfUnfulfilled(CascadingCommandTracker tracker, CascadingCommand<?> command) {
        CommandCounter commandCounter = tracker.getCommandCounter();
        return checkIfUnfulfilled(commandCounter, command.getSourceAggregateIdentifier(), command.getAllocatedToken());
    }

    public boolean checkIfUnfulfilled(CommandCounter commandCounter, String sourceId, long token) {
        for (Pair<Long,Long> segment : getFulfilledState(commandCounter, sourceId)) {
            if (token < segment.getFirst()) {
                return true;
            } else if (token <= segment.getSecond()) {
                return false;
            }
        }
        return true;
    }

    public <T extends CascadingCommand<T>> void doIfUnfulfilled(T command, CascadingCommandTracker tracker, Consumer<T> action) {
        if (checkIfUnfulfilled(tracker, command)) {
            registerFulfilled(tracker, command);
            action.accept(command);
        } else {
            log.trace("Skip already fulfilled command: {} -({})-> {}", command.getSourceAggregateIdentifier(), command.getAllocatedToken(), command.getId());
        }
    }

    private void registerFulfilled(CascadingCommandTracker tracker, CascadingCommand<?> answer) {
        registerFulfilled(tracker.getCommandCounter(), answer.getSourceAggregateIdentifier(), answer.getAllocatedToken());
    }

    public void registerFulfilled(CommandCounter commandCounter, String sourceId, long token) {
        int index = 0;
        List<Pair<Long,Long>> state = getFulfilledState(commandCounter, sourceId);
        for (Pair<Long,Long> segment : state) {
            long preceeding = segment.getFirst() - 1;
            if (token < preceeding) {
                state.add(index, Pair.of(token, token));
            } else if (token == preceeding) {
                state.set(index, Pair.of(token, segment.getSecond()));
            } else if (token == segment.getSecond() + 1) {
                int nextIndex = index + 1;
                Pair<Long, Long> nextSegment = nextIndex < state.size() ? state.get(nextIndex) : null;
                if (nextSegment != null && token == nextSegment.getFirst() - 1) {
                    state.set(index, Pair.of(segment.getFirst(), nextSegment.getSecond()));
                    state.remove(nextIndex);
                } else {
                    state.set(index, Pair.of(segment.getFirst(), token));
                }
            } else if (token > segment.getSecond()) {
                index += 1;
                continue;
            }
            break;
        }
        if (index >= state.size()) {
            state.add(Pair.of(token, token));
        }
    }

    private List<Pair<Long,Long>> getFulfilledState(CommandCounter commandCounter, String sourceId) {
        return commandCounter.getFulfilledState()
            .computeIfAbsent(sourceId, k -> new ArrayList<>(Collections.singleton(Pair.of(0L, 0L))));
    }
}
