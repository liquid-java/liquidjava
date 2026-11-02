package liquidjava.processor.state_machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import liquidjava.rj_language.Predicate;

class StateMachineTest {

    // two state sets, {open, closed} and {locked, unlocked}, combined in declaration order
    private static final State OPEN_LOCKED = state("open", "locked");
    private static final State OPEN_UNLOCKED = state("open", "unlocked");
    private static final State CLOSED_LOCKED = state("closed", "locked");
    private static final State CLOSED_UNLOCKED = state("closed", "unlocked");
    private static final StateMachine MACHINE = new StateMachine(
            List.of(OPEN_LOCKED, OPEN_UNLOCKED, CLOSED_LOCKED, CLOSED_UNLOCKED), List.of(), List.of(), Map.of(), true);

    @Test
    void statesOfOneSetConstrainOnlyThatSet() {
        assertEquals(List.of(0), MACHINE.stateSetsConstraining(Set.of(OPEN_LOCKED, OPEN_UNLOCKED)));
        assertEquals(List.of(1), MACHINE.stateSetsConstraining(Set.of(OPEN_UNLOCKED, CLOSED_UNLOCKED)));
    }

    @Test
    void singleStateConstrainsEverySet() {
        assertEquals(List.of(0, 1), MACHINE.stateSetsConstraining(Set.of(CLOSED_UNLOCKED)));
    }

    @Test
    void statesDifferingInBothSetsConstrainBoth() {
        assertEquals(List.of(0, 1), MACHINE.stateSetsConstraining(Set.of(OPEN_LOCKED, CLOSED_UNLOCKED)));
        // open || locked
        assertEquals(List.of(0, 1), MACHINE.stateSetsConstraining(Set.of(OPEN_LOCKED, OPEN_UNLOCKED, CLOSED_LOCKED)));
    }

    @Test
    void everyStateConstrainsNoSet() {
        assertEquals(List.of(), MACHINE.stateSetsConstraining(Set.copyOf(MACHINE.states())));
    }

    @Test
    void stateIsNamedInTheGivenSetsOnly() {
        assertEquals("locked", OPEN_LOCKED.name(List.of(1)));
        assertEquals("open and locked", OPEN_LOCKED.name(List.of(0, 1)));
    }

    private static State state(String... names) {
        return new State(List.of(names), new Predicate());
    }
}
