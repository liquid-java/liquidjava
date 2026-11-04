package liquidjava.processor.state_machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import liquidjava.processor.context.RefinedFunction;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.AliasInvocation;
import liquidjava.utils.constants.Keys;

class StateMachineTest {

    // two state sets, {open, closed} and {locked, unlocked}, combined in declaration order
    private static final State OPEN_LOCKED = state("open", "locked");
    private static final State OPEN_UNLOCKED = state("open", "unlocked");
    private static final State CLOSED_LOCKED = state("closed", "locked");
    private static final State CLOSED_UNLOCKED = state("closed", "unlocked");
    private static final List<List<State>> SETS = List.of(List.of(state("open"), state("closed")),
            List.of(state("locked"), state("unlocked")));
    private static final Predicate THIS = Predicate.createVar(Keys.THIS);
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
    void statesAreNamedInEverySetAsked() {
        // the states differ in the first set, which must still be named
        assertEquals(List.of(List.of("open", "unlocked"), List.of("closed", "unlocked")),
                MACHINE.describe(Set.of(OPEN_UNLOCKED, CLOSED_UNLOCKED), Set.of(0)));
        assertEquals(List.of(List.of("open"), List.of("closed")),
                MACHINE.describe(Set.copyOf(MACHINE.states()), Set.of(0)));
    }

    @Test
    void setsNamedAreThoseTheDescriptionFixes() {
        assertEquals(Set.of(1), MACHINE.stateSetsNamed(Set.of(OPEN_UNLOCKED, CLOSED_UNLOCKED)));
        assertEquals(Set.of(0, 1), MACHINE.stateSetsNamed(Set.of(OPEN_LOCKED, OPEN_UNLOCKED, CLOSED_LOCKED)));
        assertEquals(Set.of(), MACHINE.stateSetsNamed(Set.of()));
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

    // three states of one set, for calls whose effects are given
    private static final State A = state("a");
    private static final State B = state("b");
    private static final State C = state("c");
    private static final List<State> ABC = List.of(A, B, C);

    @Test
    void firstRefinementWhoseFromHoldsInEveryStateIsApplied() {
        // a -> b, then a or c -> c: in a or c the verifier applies the second, as no single state tells it apart
        CallVariant call = variant(new Effects(true, Map.of(A, Set.of(B))),
                new Effects(true, Map.of(A, Set.of(C), C, Set.of(C))));
        StateMachine machine = machine(ABC, call);
        assertTrue(machine.allows(Set.of(A, C), call));
        assertEquals(Set.of(C), machine.after(Set.of(A, C), call));
        assertEquals(Set.of(B), machine.after(Set.of(A), call));
        assertFalse(machine.allows(Set.of(B), call));
    }

    @Test
    void refinementThatMayOrMayNotApplyBeforeTheApplicableOneAllowsNothing() {
        Effects first = new Effects(true, Map.of(A, Set.of(B)));
        first.undecided.add(C);
        CallVariant call = variant(first, new Effects(true, Map.of(A, Set.of(C), C, Set.of(C))));
        assertFalse(machine(ABC, call).allows(Set.of(A, C), call));
    }

    @Test
    void callThatKeepsTheStateNeedsEachStateToSatisfySomeFrom() {
        CallVariant call = variant(new Effects(false, Map.of(A, Set.of(A))), new Effects(false, Map.of(C, Set.of(C))));
        StateMachine machine = machine(ABC, call);
        assertEquals(Set.of(A, C), machine.after(Set.of(A, C), call));
        assertFalse(machine.allows(Set.of(A, B), call));
    }

    @Test
    void callWhoseStatesAfterAreUnknownIsNotAllowed() {
        Map<State, Set<State>> after = new HashMap<>();
        after.put(A, null);
        CallVariant call = variant(new Effects(true, after));
        assertFalse(machine(ABC, call).allows(Set.of(A), call));
    }

    @Test
    void setsNamedAreThoseWithAStateInThePredicate() {
        Predicate openAndPositive = Predicate.createConjunction(inState("open", THIS),
                Predicate.createInvocation("size", THIS));
        assertEquals(Set.of(0), StateMachine.namedSets(openAndPositive, SETS));
        assertEquals(Set.of(), StateMachine.namedSets(new Predicate(), SETS));
    }

    @Test
    void aliasMayNameEverySet() {
        Predicate alias = new Predicate(new AliasInvocation("Ready", List.of(THIS.getExpression())));
        assertEquals(Set.of(0, 1), StateMachine.namedSets(alias, SETS));
        assertEquals(Set.of(0, 1), StateMachine.namedSetsBefore(alias, SETS));
    }

    @Test
    void setsNamedBeforeAreThoseNamedForOld() {
        // locked(this) && open(old(this))
        Predicate to = Predicate.createConjunction(inState("locked", THIS),
                inState("open", Predicate.createInvocation(Keys.OLD, THIS)));
        assertEquals(Set.of(0, 1), StateMachine.namedSets(to, SETS));
        assertEquals(Set.of(0), StateMachine.namedSetsBefore(to, SETS));
    }

    @Test
    void candidatesKeepTheStateOfSetsNotChanged() {
        assertEquals(List.of(OPEN_LOCKED, OPEN_UNLOCKED),
                StateMachine.keeping(MACHINE.states(), OPEN_LOCKED, Set.of(1)));
        assertEquals(List.of(OPEN_LOCKED), StateMachine.keeping(MACHINE.states(), OPEN_LOCKED, Set.of()));
    }

    private static Predicate inState(String state, Predicate object) {
        return Predicate.createInvocation(state, object);
    }

    /** Effects given by hand: the states where each refinement's from holds, and the states after the call from each */
    private record Effects(boolean changesState, Map<State, Set<State>> after, Set<State> undecided) {
        Effects(boolean changesState, Map<State, Set<State>> after) {
            this(changesState, after, new HashSet<>());
        }
    }

    private static CallVariant variant(Effects... refinements) {
        CallVariant.Effects effects = new CallVariant.Effects() {
            public int refinements() {
                return refinements.length;
            }

            public boolean changesState(int index) {
                return refinements[index].changesState();
            }

            public CallVariant.Entry entry(int index, State state) {
                Effects e = refinements[index];
                return e.after().containsKey(state) ? CallVariant.Entry.HOLDS
                        : e.undecided().contains(state) ? CallVariant.Entry.UNDECIDED : CallVariant.Entry.FAILS;
            }

            public Set<State> after(int index, State state) {
                return refinements[index].after().get(state);
            }
        };
        return new CallVariant(new RefinedFunction(), CallVariant.Kind.ANY, new Predicate(), effects);
    }

    private static StateMachine machine(List<State> states, CallVariant variant) {
        return new StateMachine(states, List.of(), List.of(), Map.of(), false, List.of(variant));
    }

    private static StateMachine machine(List<State> states) {
        return new StateMachine(states, List.of(), List.of(), Map.of(), true);
    }

    private static State state(String... names) {
        return new State(List.of(names), new Predicate());
    }
}
