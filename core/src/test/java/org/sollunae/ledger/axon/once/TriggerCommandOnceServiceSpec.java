package org.sollunae.ledger.axon.once;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.data.util.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.Assert.*;

@Slf4j
public class TriggerCommandOnceServiceSpec {

    private final TriggerCommandOnceService service = new TriggerCommandOnceService();

    @Test
    public void testRoundTrip() {
        // Aggregate "x" keeps track of cascading commands
        final CommandCounter commandCounterX = service.createCounter();
        CascadingCommandTracker trackerX = () -> commandCounterX;
        boolean unfulfilled = service.checkIfUnfulfilled(commandCounterX, "y", 1);
        assertTrue(unfulfilled);
        assertEquals(Collections.singletonList(Pair.of(0L, 0L)), commandCounterX.getFulfilledState().get("y"));

        // Previously "x" sent out an event that cause a cascading command to be sent to "z"
        TestEvent eventA = service.allocate(trackerX, TestEvent.builder().id("x").build(), "z");
        assertEquals(1, eventA.getAllocatedTokens().size());
        assertEquals(Long.valueOf(1), eventA.getAllocatedTokens().get("z"));

        // Aggregate "x" sends out an event that causes two cascading commands to be sent to other aggregates
        TestEvent eventB = service.allocate(trackerX, TestEvent.builder().id("x").build(), "y", "z");
        assertEquals(2, eventB.getAllocatedTokens().size());
        assertEquals(Long.valueOf(1), eventB.getAllocatedTokens().get("y"));
        assertEquals(Long.valueOf(2), eventB.getAllocatedTokens().get("z"));

        // Suppose the events are handles out of sequence: first eventB, then eventA

        // The event handler for eventB prepares commands for "y" and "z"

        // A command is sent with token 1 to aggregate "y"
        TestCommand command1 = TestCommand.builder().id("y").build().map(service.prepareCommand(eventB));
        assertEquals("x", command1.getSourceAggregateIdentifier());
        assertEquals("y", command1.getId());
        assertEquals(1L, command1.getAllocatedToken());

        // A command is sent with token 2 to aggregate "z"
        TestCommand command2 = TestCommand.builder().id("z").build().map(service.prepareCommand(eventB));
        assertEquals("x", command2.getSourceAggregateIdentifier());
        assertEquals("z", command2.getId());
        assertEquals(2L, command2.getAllocatedToken());

        // Aggregate "y" keeps track of cascading commands
        final CommandCounter commandCounterY = service.createCounter();
        CascadingCommandTracker trackerY = () -> commandCounterY;

        // Register fulfillment of command 1
        testDoOnce(trackerY, command1);
        assertEquals(Collections.singletonList(Pair.of(0L, 1L)), commandCounterY.getFulfilledState().get("x"));

        // Aggregate "z" keeps track of cascading commands
        final CommandCounter commandCounterZ = service.createCounter();
        CascadingCommandTracker trackerZ = () -> commandCounterZ;

        // Register fulfillment of command 2
        testDoOnce(trackerZ, command2);
        assertEquals(Arrays.asList(Pair.of(0L, 0L), Pair.of(2L, 2L)), commandCounterZ.getFulfilledState().get("x"));

        // The event handler for eventA prepares a command for "z"

        // A command is sent with token 1 to aggregate "z"
        TestCommand command3 = TestCommand.builder().id("z").build().map(service.prepareCommand(eventA));
        assertEquals("x", command3.getSourceAggregateIdentifier());
        assertEquals("z", command3.getId());
        assertEquals(1L, command3.getAllocatedToken());

        // Register fulfillment of command 3
        assertFalse(service.checkIfUnfulfilled(trackerZ, command2));
        testDoOnce(trackerZ, command3);
        assertFalse(service.checkIfUnfulfilled(trackerZ, command2));
        assertEquals(Collections.singletonList(Pair.of(0L, 2L)), commandCounterZ.getFulfilledState().get("x"));
    }

    private void testDoOnce(CascadingCommandTracker tracker, TestCommand command) {
        log.info("Command: {} -({})-> {}", command.getSourceAggregateIdentifier(), command.getAllocatedToken(), command.getId());
        AtomicBoolean flag = new AtomicBoolean(false);
        assertTrue(service.checkIfUnfulfilled(tracker, command));
        flag.set(false);
        service.doIfUnfulfilled(command, tracker, setFlag(flag, true));
        assertTrue(flag.get());
        assertFalse(service.checkIfUnfulfilled(tracker, command));
        service.doIfUnfulfilled(command, tracker, setFlag(flag, false));
        assertTrue(flag.get());
    }

    private <T> Consumer<T> setFlag(AtomicBoolean flag, boolean newValue) {
        return u -> {
            log.info("Flag: {} -> {}", flag.get(), newValue);
            flag.set(newValue);
        };
    }

    @Test
    public void testRegisterFulfilled() {
        CommandCounter commandCounter = service.createCounter();
        long y1 = service.allocate(commandCounter, "y");
        assertEquals(1, y1);
        long y2 = service.allocate(commandCounter, "y");
        assertEquals(2, y2);
        for (int i = 3; i < 10; i++) {
            service.allocate(commandCounter, "y");
        }
        assertEquals(9L, commandCounter.getAllocationCounters().get("y").get());

        boolean unfulfilled = service.checkIfUnfulfilled(commandCounter, "y", 1L);
        assertTrue(unfulfilled);
        assertEquals(Collections.singletonList(Pair.of(0L, 0L)), commandCounter.getFulfilledState().get("y"));
        service.registerFulfilled(commandCounter, "y", 8L);
        assertEquals(Arrays.asList(Pair.of(0L, 0L), Pair.of(8L, 8L)), commandCounter.getFulfilledState().get("y"));
        service.registerFulfilled(commandCounter, "y", 6L);
        assertEquals(Arrays.asList(Pair.of(0L, 0L), Pair.of(6L, 6L), Pair.of(8L, 8L)), commandCounter.getFulfilledState().get("y"));
        service.registerFulfilled(commandCounter, "y", 1L);
        assertEquals(Arrays.asList(Pair.of(0L, 1L), Pair.of(6L, 6L), Pair.of(8L, 8L)), commandCounter.getFulfilledState().get("y"));
        service.registerFulfilled(commandCounter, "y", 4L);
        assertEquals(Arrays.asList(Pair.of(0L, 1L), Pair.of(4L, 4L), Pair.of(6L, 6L), Pair.of(8L, 8L)), commandCounter.getFulfilledState().get("y"));
        service.registerFulfilled(commandCounter, "y", 3L);
        assertEquals(Arrays.asList(Pair.of(0L, 1L), Pair.of(3L, 4L), Pair.of(6L, 6L), Pair.of(8L, 8L)), commandCounter.getFulfilledState().get("y"));
        service.registerFulfilled(commandCounter, "y", 2L);
        assertEquals(Arrays.asList(Pair.of(0L, 4L), Pair.of(6L, 6L), Pair.of(8L, 8L)), commandCounter.getFulfilledState().get("y"));
        service.registerFulfilled(commandCounter, "y", 7L);
        assertEquals(Arrays.asList(Pair.of(0L, 4L), Pair.of(6L, 8L)), commandCounter.getFulfilledState().get("y"));
        service.registerFulfilled(commandCounter, "y", 5L);
        assertEquals(Collections.singletonList(Pair.of(0L, 8L)), commandCounter.getFulfilledState().get("y"));
        service.registerFulfilled(commandCounter, "y", 9L);
        assertEquals(Collections.singletonList(Pair.of(0L, 9L)), commandCounter.getFulfilledState().get("y"));
    }


    @Test
    public void testCheckIfUnfulfilled() {
        CommandCounter commandCounter = service.createCounter();
        long y1 = service.allocate(commandCounter, "y");
        assertEquals(1, y1);
        long y2 = service.allocate(commandCounter, "y");
        assertEquals(2, y2);
        for (int i = 3; i < 10; i++) {
            service.allocate(commandCounter, "y");
        }
        assertEquals(9L, commandCounter.getAllocationCounters().get("y").get());

        boolean unfulfilled = service.checkIfUnfulfilled(commandCounter, "y", 1);
        assertTrue(unfulfilled);
        assertEquals(Collections.singletonList(Pair.of(0L, 0L)), commandCounter.getFulfilledState().get("y"));
        Set<Long> fulfilled = new HashSet<>(Arrays.asList(2L, 3L, 4L, 7L, 9L));
        for (Long item : fulfilled) {
            service.registerFulfilled(commandCounter, "y", item);
        }
        for (long i = 1; i < 12; i++) {
            assertEquals(String.valueOf(i) + ": ", !fulfilled.contains(i), service.checkIfUnfulfilled(commandCounter, "y", i));
        }
    }
}
