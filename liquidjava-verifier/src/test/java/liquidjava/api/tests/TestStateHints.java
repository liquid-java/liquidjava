package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.StateRefinementError;
import liquidjava.diagnostics.hints.Hint;
import liquidjava.diagnostics.hints.Hint.Kind;

class TestStateHints {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";

    @Test
    void missingCallIsSuggestedWithTheCallsAllowedNow() {
        assertHints("ErrorStateHintCallSequence.java",
                new Hint(Kind.CALL_SEQUENCE, "Call f.open() before this call to move f from created to open"),
                new Hint(Kind.AVAILABLE_CALLS, "In state created, f can call: open()"));
    }

    @Test
    void missingCallsAreSuggestedInOrder() {
        assertHints("ErrorStateHintTwoCalls.java",
                new Hint(Kind.CALL_SEQUENCE,
                        "Call r.prepare(), then r.start() before this call to move r from idle to running"),
                new Hint(Kind.AVAILABLE_CALLS, "In state idle, r can call: prepare()"));
    }

    @Test
    void callSequenceNamesOnlyTheStateSetsItChanges() {
        assertHints("ErrorStateHintMultipleSets.java",
                new Hint(Kind.CALL_SEQUENCE, "Call d.unlock() before this call to move d from locked to unlocked"),
                new Hint(Kind.AVAILABLE_CALLS, "In state closed and locked, d can call: unlock()"));
    }

    @Test
    void finalStateSuggestsCreatingANewObject() {
        assertHints("ErrorStateHintFinalState.java", new Hint(Kind.UNREACHABLE_STATE,
                "No sequence of calls moves f from closed to open. Create a new object instead with new ErrorStateHintFinalState()"),
                new Hint(Kind.FINAL_STATE,
                        "f is in state closed, where none of its methods with state refinements can be called"));
    }

    @Test
    void transitionDependingOnArgumentsIsSuggestedWithItsConditionButPreventsClaimingStateIsFinal() {
        assertHints("ErrorStateHintIncomplete.java", new Hint(Kind.CALL_SEQUENCE,
                "Call f.reopen(int attempts) with attempts > 0 before this call to move f from closed to open"));
    }

    @Test
    void subclassTransitionsPreventClaimingStateIsUnreachable() {
        assertHints("ErrorStateHintSubclass.java");
    }

    @Test
    void subclassReceiverCanUseItsOwnTransitionsOnInheritedStates() {
        assertHints("ErrorStateHintInheritedStates.java",
                new Hint(Kind.CALL_SEQUENCE, "Call s.bc() before this call to move s from b to c"),
                new Hint(Kind.AVAILABLE_CALLS, "In state b, s can call: bc()"));
    }

    @Test
    void overrideInTheReceiversClassReplacesTheInheritedTransition() {
        assertHints("ErrorStateHintOverride.java",
                new Hint(Kind.CALL_SEQUENCE, "Call s.next() before this call to move s from a to c"),
                new Hint(Kind.AVAILABLE_CALLS, "In state a, s can call: next()"));
    }

    @Test
    void stateSetAndMethodsFromSpecsOfTheReceiversSuperclasses() {
        assertHints("classes/state_hints_grandchild_spec_error",
                new Hint(Kind.CALL_SEQUENCE,
                        "Call window.dispose() before this call to move window from displayable to notDisplayable"),
                new Hint(Kind.AVAILABLE_CALLS, "In state displayable, window can call: dispose(), pack()"));
    }

    @Test
    void superclassRefinementOfAMethodTheReceiversClassRedeclaresIsNotSuggested() {
        assertHints("classes/state_hints_redeclared_method_error",
                new Hint(Kind.CALL_SEQUENCE,
                        "Call host.dispose() before this call to move host from displayable to notDisplayable"),
                new Hint(Kind.AVAILABLE_CALLS,
                        "In state displayable, host can call: dispose(), pack() (and possibly others)"));
    }

    @Test
    void stateSetAndMethodsFromTheReceiversSuperclassSpec() {
        assertHints("classes/state_hints_superclass_spec_error",
                new Hint(Kind.CALL_SEQUENCE,
                        "Call host.dispose() before this call to move host from displayable to notDisplayable"),
                new Hint(Kind.AVAILABLE_CALLS, "In state displayable, host can call: dispose(), pack()"));
    }

    @Test
    void ownGenericRefinementOfAnInheritedMethodIsNotSuggested() {
        assertHints("classes/state_hints_generic_inherited_method_error");
    }

    @Test
    void statesOfAnotherClassWithTheSimpleNameOfASupertypeAreLeftOut() {
        assertHints("classes/state_hints_same_simple_name_error",
                new Hint(Kind.CALL_SEQUENCE, "Call l.add(E) before this call to move l from empty to nonEmpty"),
                new Hint(Kind.AVAILABLE_CALLS, "In state empty, l can call: add(E)"));
    }

    @Test
    void stateDependingOnThePathAsksForTheSameStateOnEveryPath() {
        assertHints("ErrorStateHintPathDependent.java", new Hint(Kind.PATH_DEPENDENT_STATE,
                "f can be in created or open here, depending on the path taken, and no sequence of calls moves all of them to open. "
                        + "Make f reach this line in the same state on every path"));
    }

    @Test
    void stateDependingOnThePathInEveryStateIsNotReportedAsUnknown() {
        assertHints("ErrorStateHintJoinOfAllStates.java", new Hint(Kind.PATH_DEPENDENT_STATE,
                "f can be in created or open here, depending on the path taken, and no sequence of calls moves all of them to open. "
                        + "Make f reach this line in the same state on every path"));
    }

    @Test
    void callAllowedByDifferentRefinementsInEachStateIsNotSuggested() {
        assertHints("ErrorStateHintSeveralRefinements.java", new Hint(Kind.PATH_DEPENDENT_STATE,
                "f can be in created or open here, depending on the path taken, and no sequence of calls moves all of them to open. "
                        + "Make f reach this line in the same state on every path"));
    }

    @Test
    void receiverThatIsNotAVariableGetsNoHints() {
        assertHints("ErrorStateHintMethodCallReceiver.java");
    }

    @Test
    void unknownStateDoesNotSuggestACallThatNeedsAnotherUnknownState() {
        assertHints("ErrorStateHintUnknownState.java",
                new Hint(Kind.NEEDED_STATE, "The state of f is unknown here, and this call needs it to be open"));
    }

    @Test
    void unknownStateOfAnExternalClassNamesOnlyTheCheckedStateSet() {
        assertHints("classes/state_hint_undo_manager_error",
                new Hint(Kind.NEEDED_STATE,
                        "The state of undoManager is unknown here, and this call needs it to be redoable"),
                new Hint(Kind.GUARD,
                        "Guard this call with if (undoManager.canRedo()): when it returns true, undoManager is redoable"));
    }

    @Test
    void unknownStateSuggestsCallsAllowedInAnyState() {
        assertHints("ErrorStateHintUnknownAnyState.java",
                new Hint(Kind.NEEDED_STATE, "The state of f is unknown here, and this call needs it to be open"),
                new Hint(Kind.CALL_SEQUENCE,
                        "Call f.reset(), then f.open() before this call to move f from any state to open"));
    }

    @Test
    void statesAreNamedOnlyInTheStateSetsThatMatter() {
        List<StateRefinementError> errors = errors("ErrorStateHintNaming.java", 3);
        assertHints(errors.get(0), new Hint(Kind.NEEDED_STATE,
                "The state of s is unknown here, and this call needs it to be open or loud"));
        assertHints(errors.get(1),
                new Hint(Kind.NEEDED_STATE, "The state of t is unknown here, and this call needs it to be a or b. "
                        + "Objects created with new ErrorStateHintNamingToggle() start in such a state"));
        assertHints(errors.get(2),
                new Hint(Kind.UNREACHABLE_STATE, "No sequence of calls moves this from notRedoable to redoable"),
                new Hint(Kind.AVAILABLE_CALLS, "In state notRedoable, this can call: check()"));
    }

    @Test
    void failedCheckOfTheReceiverIsNotAnUnknownState() {
        List<StateRefinementError> errors = errors("classes/state_hint_checked_error", 3);
        // the check is suggested even where it was made with the opposite result
        assertHints(errors.get(0),
                new Hint(Kind.NEEDED_STATE,
                        "What is known here does not decide the state of it, and this call needs it to be hasMore"),
                new Hint(Kind.GUARD, "Guard this call with if (it.hasNext()): when it returns true, it is hasMore"));
        assertHints(errors.get(1),
                new Hint(Kind.NEEDED_STATE,
                        "What is known here does not decide the state of same, and this call needs it to be hasMore"),
                new Hint(Kind.GUARD,
                        "Guard this call with if (same.hasNext()): when it returns true, same is hasMore"));
        assertHints(errors.get(2),
                new Hint(Kind.NEEDED_STATE, "The state of input is unknown here, and this call needs it to be hasMore"),
                new Hint(Kind.GUARD,
                        "Guard this call with if (input.hasNext()): when it returns true, input is hasMore"));
        assertEquals(List.of(Optional.empty(), Optional.empty(), Optional.of(ErrorCategory.UNKNOWN)),
                errors.stream().map(LJError::getCategory).toList());
    }

    @Test
    void joinsAreNotTakenForAnUnknownState() {
        List<StateRefinementError> errors = errors("ErrorStateHintJoins.java", 2);
        assertHints(errors.get(0), new Hint(Kind.UNREACHABLE_STATE,
                "No sequence of calls moves d from closed and unlocked or closed and locked or opened and unlocked "
                        + "or opened and locked to closed and unlocked. "
                        + "Create a new object instead with new ErrorStateHintJoins()"));
        assertHints(errors.get(1), new Hint(Kind.UNREACHABLE_STATE,
                "No sequence of calls moves d from closed and unlocked or opened and unlocked to closed and unlocked. "
                        + "Create a new object instead with new ErrorStateHintJoins()"),
                new Hint(Kind.AVAILABLE_CALLS,
                        "In state closed and unlocked or opened and unlocked, d can call: lock()"));
        assertEquals(Optional.empty(), errors.get(0).getCategory());
    }

    @Test
    void onlyConstructorsThatCanBeCalledAreSuggested() {
        List<StateRefinementError> errors = errors("ErrorStateHintConstructors.java", 4);
        assertHints(errors.get(0),
                new Hint(Kind.NEEDED_STATE, "The state of value is unknown here, and this call needs it to be ready. "
                        + "Objects created with new ErrorStateHintConstructors() start in such a state"));
        assertHints(errors.get(1),
                new Hint(Kind.NEEDED_STATE, "The state of this is unknown here, and this call needs it to be ready"));
        assertHints(errors.get(2),
                new Hint(Kind.NEEDED_STATE, "The state of value is unknown here, and this call needs it to be ready"));
        assertHints(errors.get(3),
                new Hint(Kind.NEEDED_STATE, "The state of value is unknown here, and this call needs it to be ready"));
    }

    @Test
    void onlyPublicInheritedMethodsAreSuggested() {
        List<StateRefinementError> errors = errors("classes/state_hints_inaccessible_inherited_error", 2);
        // the files are verified in no fixed order
        assertEquals(Set.of(
                List.of(new Hint(Kind.CALL_SEQUENCE,
                        "Call hatch.openPublicly() before this call to move hatch from closed to open"),
                        new Hint(Kind.AVAILABLE_CALLS,
                                "In state closed, hatch can call: openPublicly() (and possibly others)")),
                List.of(new Hint(Kind.CALL_SEQUENCE,
                        "Call door.push() before this call to move door from shut to ajar"),
                        new Hint(Kind.AVAILABLE_CALLS, "In state shut, door can call: push() (and possibly others)"))),
                errors.stream().map(StateRefinementError::getHints).collect(Collectors.toSet()));
    }

    @Test
    void argumentThatDecidedTheStateIsShownWithAValueThatWouldWork() {
        assertHints("ErrorArgumentPicksTheState.java", new Hint(Kind.DECIDING_ARGUMENT,
                "buffer.reset(boolean keep) on line 20 decides the state of buffer: with keep == true, this call would be allowed"));
    }

    @Test
    void deciderIsTheLastCallThatChangedTheCheckedStateSet() {
        assertHints("ErrorArgumentPicksTheStateEarlier.java", new Hint(Kind.DECIDING_ARGUMENT,
                "param.setTilingMode(int mode) on line 25 decides the state of param: with mode == 2, this call would be allowed"));
    }

    @Test
    void checksAreSuggestedOnlyWhenTheRefinementsProveThem() {
        List<StateRefinementError> errors = errors("ErrorStateHintGuards.java", 11);
        assertHints(errors.get(0),
                new Hint(Kind.NEEDED_STATE,
                        "The state of c is unknown here, and this call needs it to be open. "
                                + "Objects created with new GuardChannel() start in such a state"),
                new Hint(Kind.GUARD, "Guard this call with if (!c.isClosed()): when it returns false, c is open"));
        assertHints(errors.get(1),
                new Hint(Kind.GUARD, "Guard this call with if (l.tryLock(...)), "
                        + "filling in the arguments of tryLock(long timeout): when it returns true, l is locked"),
                new Hint(Kind.AVAILABLE_CALLS, "In state unlocked, l can call: tryLock(long) (and possibly others)"));
        assertHints(errors.get(2),
                new Hint(Kind.NEEDED_STATE, "The state of p is unknown here, and this call needs it to be ready"));
        assertHints(errors.get(3),
                new Hint(Kind.NEEDED_STATE, "The state of w is unknown here, and this call needs it to be accept"));
        assertHints(errors.get(4),
                new Hint(Kind.GUARD, "Guard this call with if (w.canWrite()): when it returns true, w is accept"),
                new Hint(Kind.AVAILABLE_CALLS, "In state start, w can call: canWrite() (and possibly others)"));
        assertHints(errors.get(5), new Hint(Kind.NEEDED_STATE,
                "The state of b is unknown here, and this call needs it to be undoable and redoable"));
        assertHints(errors.get(6),
                new Hint(Kind.NEEDED_STATE, "The state of p is unknown here, and this call needs it to be armed"));
        assertHints(errors.get(7),
                new Hint(Kind.NEEDED_STATE, "The state of m is unknown here, and this call needs it to be s1"));
        assertHints(errors.get(8),
                new Hint(Kind.NEEDED_STATE, "The state of t is unknown here, and this call needs it to be fresh. "
                        + "Objects created with new GuardToken() start in such a state"));
        assertHints(errors.get(9),
                new Hint(Kind.AVAILABLE_CALLS, "In state live, r can call: read() (and possibly others)"));
        assertHints(errors.get(10),
                new Hint(Kind.NEEDED_STATE, "The state of g is unknown here, and this call needs it to be running"));
    }

    @Test
    void argumentConditionsAreSuggestedOnlyWhenEveryArgumentMeetingThemWorks() {
        List<StateRefinementError> errors = errors("ErrorStateHintArgumentConditions.java", 7);
        assertHints(errors.get(0),
                new Hint(Kind.CALL_SEQUENCE,
                        "Call r.set(int lo, int hi) with lo < hi before this call to move r from low to high"),
                new Hint(Kind.AVAILABLE_CALLS, "In state low, r can call: set(int, int) (and possibly others)"));
        assertHints(errors.get(1), new Hint(Kind.CALL_SEQUENCE,
                "Call chain.pick(int k) with k == 1, then chain.next() before this call to move chain from a to c"),
                new Hint(Kind.AVAILABLE_CALLS, "In state a, chain can call: pick(int) (and possibly others)"));
        assertHints(errors.get(2), new Hint(Kind.CALL_SEQUENCE,
                "Call l.setLimit(int limit) with limit <= 0 before this call to move l from bounded to unbounded"),
                new Hint(Kind.AVAILABLE_CALLS, "In state bounded, l can call: setLimit(int) (and possibly others)"));
        // no condition, and no value of the earlier call's argument, is claimed to allow the call
        for (StateRefinementError error : errors.subList(3, 5))
            assertHints(error,
                    new Hint(Kind.AVAILABLE_CALLS, "In state b, l can call: set(int) (and possibly others)"));
        assertHints(errors.get(5),
                new Hint(Kind.AVAILABLE_CALLS, "In state small, s can call: grow(int) (and possibly others)"));
        assertHints(errors.get(6),
                new Hint(Kind.AVAILABLE_CALLS, "In state empty, b can call: fill(ConditionBin) (and possibly others)"));
    }

    @Test
    void callWhoseStateDependsOnItsArgumentIsSuggestedWithTheCondition() {
        assertHints("classes/state_hint_compression_mode_error", new Hint(Kind.CALL_SEQUENCE,
                "Call wParam.setCompressionMode(int mode) with mode == 2 before this call to move wParam from startCompression to compressionExplicit"),
                new Hint(Kind.AVAILABLE_CALLS,
                        "In state startCompression, wParam can call: setCompressionMode(int) (and possibly others)"));
    }

    @Test
    void checksOfTheStudysUndoManagerAreSuggestedOneAfterTheOther() {
        List<StateRefinementError> errors = errors("classes/state_hint_study_undo_manager_error", 2);
        assertHints(errors.get(0),
                new Hint(Kind.NEEDED_STATE,
                        "The state of undoManager is unknown here, and this call needs it to be redoable"),
                new Hint(Kind.GUARD,
                        "Guard this call with if (undoManager.canRedo()): when it returns true, undoManager is redoable"));
        assertHints(errors.get(1),
                new Hint(Kind.NEEDED_STATE,
                        "The state of undoManager is unknown here, and this call needs it to be undoable"),
                new Hint(Kind.GUARD,
                        "Guard this call with if (undoManager.canUndo()): when it returns true, undoManager is undoable"));
    }

    @Test
    void foundOfTheStudysUndoManagerOmitsTheResultsOfOtherCalls() {
        StateRefinementError error = errors("classes/state_hint_study_undo_manager_error", 2).get(0);
        assertEquals("expected redoable(undoManager) but could not prove it",
                error.getMessage().replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", ""));
    }

    @Test
    void foundKeepsANamedStateSetByItsName() {
        StateRefinementError error = errors("ErrorStateHintMultipleSets.java", 1).get(0);
        assertEquals(
                "found closed(d@26) && locked(d) && doorState(d) == doorState(d@26) but expected closed(d) && unlocked(d)",
                error.getMessage().replaceAll("[⁰¹²³⁴⁵⁶⁷⁸⁹]", ""));
    }

    @Test
    void classWithoutStateSetKeepsTheGeneralHint() {
        assertHints("ErrorUnconstrainedStateRefinement.java", Hint.general(
                "No state information is known for this object. Make sure its state is initialized and available at this point."));
    }

    private static void assertHints(String test, Hint... expected) {
        assertHints(errors(test, 1).get(0), expected);
    }

    private static void assertHints(StateRefinementError error, Hint... expected) {
        assertEquals(List.of(expected), error.getHints());
    }

    /** The {@code count} errors {@code test} reports, in the order they are reported */
    private static List<StateRefinementError> errors(String test, int count) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream().toList();
        assertEquals(count, errors.size(), "Unexpected number of errors from " + test);
        return errors.stream().map(e -> assertInstanceOf(StateRefinementError.class, e)).toList();
    }
}
