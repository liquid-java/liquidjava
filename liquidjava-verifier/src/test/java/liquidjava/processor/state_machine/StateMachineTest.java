package liquidjava.processor.state_machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
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
    private static final StateMachine MACHINE = machine(
            List.of(OPEN_LOCKED, OPEN_UNLOCKED, CLOSED_LOCKED, CLOSED_UNLOCKED));

    @Test
    void statesDifferingOnlyInOneSetAreNamedByTheOther() {
        assertEquals(List.of(List.of("open")), MACHINE.describe(Set.of(OPEN_LOCKED, OPEN_UNLOCKED)));
        assertEquals(List.of(List.of("unlocked")), MACHINE.describe(Set.of(OPEN_UNLOCKED, CLOSED_UNLOCKED)));
    }

    @Test
    void singleStateIsNamedInEverySet() {
        assertEquals(List.of(List.of("closed", "unlocked")), MACHINE.describe(Set.of(CLOSED_UNLOCKED)));
    }

    @Test
    void statesDifferingInBothSetsAreNamedInBoth() {
        assertEquals(List.of(List.of("open", "locked"), List.of("closed", "unlocked")),
                MACHINE.describe(Set.of(CLOSED_UNLOCKED, OPEN_LOCKED)));
    }

    @Test
    void disjunctionAcrossSetsNamesEachStateAlone() {
        // open || locked
        assertEquals(List.of(List.of("open"), List.of("locked")),
                MACHINE.describe(Set.of(OPEN_LOCKED, OPEN_UNLOCKED, CLOSED_LOCKED)));
    }

    @Test
    void everyStateIsNamedStateByState() {
        assertEquals(List.of(List.of("open", "locked"), List.of("open", "unlocked"), List.of("closed", "locked"),
                List.of("closed", "unlocked")), MACHINE.describe(Set.copyOf(MACHINE.states())));
    }

    @Test
    void noStatesAreNamedByNothing() {
        assertEquals(List.of(), MACHINE.describe(Set.of()));
    }

    @Test
    void disjunctionOverThreeSetsNamesOnlyTheCheckedStates() {
        // {start, canEdit, end} x {undoable, notUndoable} x {redoable, notRedoable}, and undoable || redoable
        List<State> states = new ArrayList<>();
        for (String edit : List.of("start", "canEdit", "end"))
            for (String undo : List.of("undoable", "notUndoable"))
                for (String redo : List.of("redoable", "notRedoable"))
                    states.add(state(edit, undo, redo));
        Set<State> goal = Set.copyOf(states.stream()
                .filter(s -> s.names().get(1).equals("undoable") || s.names().get(2).equals("redoable")).toList());
        assertEquals(List.of(List.of("undoable"), List.of("redoable")), machine(states).describe(goal));
    }

    @Test
    void statesOfOneSetAreNamedInDeclarationOrder() {
        List<State> states = List.of(state("s0"), state("s1"), state("s2"), state("s3"));
        assertEquals(List.of(List.of("s1"), List.of("s3")),
                machine(states).describe(Set.of(states.get(3), states.get(1))));
    }

    private static StateMachine machine(List<State> states) {
        return new StateMachine(states, List.of(), List.of(), Map.of(), true);
    }

    private static State state(String... names) {
        return new State(List.of(names), new Predicate());
    }
}
