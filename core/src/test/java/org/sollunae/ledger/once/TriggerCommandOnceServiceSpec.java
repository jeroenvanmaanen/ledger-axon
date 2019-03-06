package org.sollunae.ledger.once;

import org.junit.Test;
import org.sollunae.ledger.axon.once.CascadingCommandTracker;
import org.sollunae.ledger.axon.once.CommandCounter;
import org.sollunae.ledger.axon.once.RegisterFulfilledCommand;
import org.sollunae.ledger.axon.once.TriggerCommandOnceService;
import org.springframework.data.util.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class TriggerCommandOnceServiceSpec {

    private final TriggerCommandOnceService service = new TriggerCommandOnceService();

    @Test
    public void testRoundTrip() {
        // Aggregate "x" keeps track of cascading commands
        final CommandCounter commandCounter = service.createCounter();
        CascadingCommandTracker tracker = () -> commandCounter;
        assertEquals(Collections.singletonList(Pair.of(0L, 0L)), commandCounter.getState());

        // Aggregate "x" sends out an event that causes two cascading commands to be sent to other aggregates
        TestEvent event = service.allocate(tracker, TestEvent.builder().build(), 2);
        assertEquals(Pair.of(1L, 2L), event.getAllocatedTokens());

        // A command is sent with token 1 to aggregate "y"
        TestCommand command1 = TestCommand.builder().id("y").sourceAggregateIdentifier("x").allocatedToken(1L).build();
        // A command is sent with token 2 to aggregate "z"
        TestCommand command2 = TestCommand.builder().id("z").sourceAggregateIdentifier("x").allocatedToken(2L).build();

        RegisterFulfilledCommand answer2 = service.createAnswer(command2);
        service.registerFulfilled(tracker, answer2);
        assertEquals(Arrays.asList(Pair.of(0L, 0L), Pair.of(2L, 2L)), commandCounter.getState());

        RegisterFulfilledCommand answer1 = service.createAnswer(command1);
        service.registerFulfilled(tracker, answer1);
        assertEquals(Collections.singletonList(Pair.of(0L, 2L)), commandCounter.getState());
    }

    @Test
    public void testRegisterFulfilled() {
        CommandCounter commandCounter = service.createCounter();
        long y1 = service.allocate(commandCounter);
        assertEquals(1, y1);
        long y2 = service.allocate(commandCounter);
        assertEquals(2, y2);
        Pair<Long,Long> segment = service.allocate(commandCounter, 7L);
        assertEquals(Pair.of(3L, 9L), segment);

        assertEquals(Collections.singletonList(Pair.of(0L, 0L)), commandCounter.getState());
        service.registerFulfilled(commandCounter, 8L);
        assertEquals(Arrays.asList(Pair.of(0L, 0L), Pair.of(8L, 8L)), commandCounter.getState());
        service.registerFulfilled(commandCounter, 6L);
        assertEquals(Arrays.asList(Pair.of(0L, 0L), Pair.of(6L, 6L), Pair.of(8L, 8L)), commandCounter.getState());
        service.registerFulfilled(commandCounter, 1L);
        assertEquals(Arrays.asList(Pair.of(0L, 1L), Pair.of(6L, 6L), Pair.of(8L, 8L)), commandCounter.getState());
        service.registerFulfilled(commandCounter, 4L);
        assertEquals(Arrays.asList(Pair.of(0L, 1L), Pair.of(4L, 4L), Pair.of(6L, 6L), Pair.of(8L, 8L)), commandCounter.getState());
        service.registerFulfilled(commandCounter, 3L);
        assertEquals(Arrays.asList(Pair.of(0L, 1L), Pair.of(3L, 4L), Pair.of(6L, 6L), Pair.of(8L, 8L)), commandCounter.getState());
        service.registerFulfilled(commandCounter, 2L);
        assertEquals(Arrays.asList(Pair.of(0L, 4L), Pair.of(6L, 6L), Pair.of(8L, 8L)), commandCounter.getState());
        service.registerFulfilled(commandCounter, 7L);
        assertEquals(Arrays.asList(Pair.of(0L, 4L), Pair.of(6L, 8L)), commandCounter.getState());
        service.registerFulfilled(commandCounter, 5L);
        assertEquals(Collections.singletonList(Pair.of(0L, 8L)), commandCounter.getState());
        service.registerFulfilled(commandCounter, 9L);
        assertEquals(Collections.singletonList(Pair.of(0L, 9L)), commandCounter.getState());
    }


    @Test
    public void testCheckIfUnfulfilled() {
        CommandCounter commandCounter = service.createCounter();
        long y1 = service.allocate(commandCounter);
        assertEquals(1, y1);
        long y2 = service.allocate(commandCounter);
        assertEquals(2, y2);
        Pair<Long,Long> segment = service.allocate(commandCounter, 9L);
        assertEquals(Pair.of(3L, 11L), segment);

        assertEquals(Collections.singletonList(Pair.of(0L, 0L)), commandCounter.getState());
        Set<Long> fulfilled = new HashSet<>(Arrays.asList(2L, 3L, 4L, 7L, 9L));
        for (Long item : fulfilled) {
            service.registerFulfilled(commandCounter, item);
        }
        for (long i = 1; i < 12; i++) {
            assertEquals(String.valueOf(i) + ": ", !fulfilled.contains(i), service.checkIfUnfulfilled(commandCounter, i));
        }
    }
}
