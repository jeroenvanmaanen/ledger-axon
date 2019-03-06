package org.sollunae.ledger.axon.once;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class TriggerCommandOnceService {

    public CommandCounter createCounter() {
        return CommandCounter.builder()
            .counter(new AtomicLong())
            .state(new ArrayList<>(Collections.singleton(Pair.of(0L,0L))))
            .build();
    }

    public RegisterFulfilledCommand createAnswer(CascadingCommand command) {
        return RegisterFulfilledCommand.builder()
            .id(command.getSourceAggregateIdentifier())
            .token(command.getAllocatedToken())
            .build();
    }

    public <T> T allocate(CascadingCommandTracker tracker, WithAllocatedTokens<T> event) {
        long token = allocate(tracker.getCommandCounter());
        return event.withAllocatedToken(token);
    }

    public <T> T allocate(CascadingCommandTracker tracker, WithAllocatedTokens<T> event, long amount) {
        Pair<Long,Long> tokens = allocate(tracker.getCommandCounter(), amount);
        return event.withAllocatedTokens(tokens);
    }

    public long allocate(CommandCounter commandCounter) {
        return commandCounter.getCounter().incrementAndGet();
    }

    public Pair<Long,Long> allocate(CommandCounter commandCounter, long amount) {
        if (amount < 1) {
            return null;
        }
        long last = commandCounter.getCounter().addAndGet(amount);
        return Pair.of(last - amount + 1, last);
    }

    public boolean checkIfUnfulfilled(CommandCounter commandCounter, long token) {
        for (Pair<Long,Long> segment : commandCounter.getState()) {
            if (token < segment.getFirst()) {
                return true;
            } else if (token <= segment.getSecond()) {
                return false;
            }
        }
        return true;
    }

    public void registerFulfilled(CommandCounter commandCounter, long token) {
        int index = 0;
        List<Pair<Long,Long>> state = commandCounter.getState();
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

    public void registerFulfilled(CascadingCommandTracker tracker, RegisterFulfilledCommand answer) {
        registerFulfilled(tracker.getCommandCounter(), answer.getToken());
    }
}
