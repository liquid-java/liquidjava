package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
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
    void statesOfMultipleStateSetsAreCombined() {
        assertHints("ErrorStateHintMultipleSets.java",
                new Hint(Kind.CALL_SEQUENCE,
                        "Call d.unlock() before this call to move d from closed and locked to closed and unlocked"),
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
    void transitionDependingOnArgumentsPreventsClaimingStateIsFinal() {
        assertHints("ErrorStateHintIncomplete.java");
    }

    @Test
    void subclassTransitionsPreventClaimingStateIsUnreachable() {
        assertHints("ErrorStateHintSubclass.java");
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
        assertHints("classes/state_hint_undo_manager_error", new Hint(Kind.NEEDED_STATE,
                "The state of undoManager is unknown here, and this call needs it to be redoable"));
    }

    @Test
    void unknownStateWithThreeStateSetsNamesOnlyTheCheckedOne() {
        assertHints("ErrorStateHintUnknownThreeSets.java", new Hint(Kind.NEEDED_STATE,
                "The state of history is unknown here, and this call needs it to be redoable"));
    }

    @Test
    void unknownStateSuggestsCallsAllowedInAnyState() {
        assertHints("ErrorStateHintUnknownAnyState.java",
                new Hint(Kind.NEEDED_STATE, "The state of f is unknown here, and this call needs it to be open"),
                new Hint(Kind.CALL_SEQUENCE,
                        "Call f.reset(), then f.open() before this call to move f from any state to open"));
    }

    @Test
    void knownStateStartsTheSequenceFromThatState() {
        assertHints("ErrorStateHintKnownAnyState.java",
                new Hint(Kind.CALL_SEQUENCE,
                        "Call f.reset(), then f.open() before this call to move f from closed to open"),
                new Hint(Kind.AVAILABLE_CALLS, "In state closed, f can call: reset()"));
    }

    @Test
    void unknownStateNamesEveryStateSetTheCallChecks() {
        assertHints("ErrorStateHintUnknownTwoSetGoal.java",
                new Hint(Kind.NEEDED_STATE,
                        "The state of door is unknown here, and this call needs it to be closed and unlocked. "
                                + "Objects created with new ErrorStateHintUnknownTwoSetGoal() start in such a state"),
                new Hint(Kind.CALL_SEQUENCE,
                        "Call door.reset() before this call to move door from any state to closed and unlocked"));
    }

    @Test
    void unknownStateOfAFieldSuggestsCallsAllowedInAnyState() {
        assertHints("ErrorStateHintUnknownField.java",
                new Hint(Kind.NEEDED_STATE, "The state of door is unknown here, and this call needs it to be opened"),
                new Hint(Kind.CALL_SEQUENCE,
                        "Call door.forceOpen() before this call to move door from any state to opened"));
    }

    @Test
    void unknownStateNamesEveryStateTheCallAccepts() {
        assertHints("ErrorStateHintUnknownDisjunction.java", new Hint(Kind.NEEDED_STATE,
                "The state of s is unknown here, and this call needs it to be open or ready"));
    }

    @Test
    void disjunctionAcrossStateSetsNamesEachStateAlone() {
        assertHints("ErrorStateHintUnknownCrossDisjunction.java", new Hint(Kind.NEEDED_STATE,
                "The state of s is unknown here, and this call needs it to be open or loud"));
    }

    @Test
    void oneRefinementPerStateSetNamesEachStateAlone() {
        assertHints("ErrorStateHintUnknownTwoRefinements.java", new Hint(Kind.NEEDED_STATE,
                "The state of history is unknown here, and this call needs it to be undoable or redoable"));
    }

    @Test
    void callAllowedInEveryStateByDifferentRefinementsStillNamesTheStatesAndConstructors() {
        assertHints("ErrorStateHintUnknownToggle.java",
                new Hint(Kind.NEEDED_STATE, "The state of t is unknown here, and this call needs it to be up or down. "
                        + "Objects created with new ErrorStateHintUnknownToggle() start in such a state"));
    }

    @Test
    void unknownStateDoesNotSuggestSequencesLongerThanTheLimit() {
        assertHints("ErrorStateHintUnknownLongPath.java",
                new Hint(Kind.NEEDED_STATE, "The state of x is unknown here, and this call needs it to be s3"));
    }

    @Test
    void unknownStateSuggestsEverySequenceOfTheShortestLength() {
        assertHints("ErrorStateHintUnknownSeveralWays.java",
                new Hint(Kind.NEEDED_STATE, "The state of x is unknown here, and this call needs it to be s1 or s2"),
                new Hint(Kind.CALL_SEQUENCE, "Call x.one() before this call to move x from any state to s1"),
                new Hint(Kind.CALL_SEQUENCE, "Call x.two() before this call to move x from any state to s2"));
    }

    @Test
    void thisReceiverIsNotToldToCreateANewObject() {
        assertHints("ErrorStateHintUnknownThis.java",
                new Hint(Kind.NEEDED_STATE, "The state of this is unknown here, and this call needs it to be open"));
    }

    @Test
    void stateCheckedByAnEarlierCallIsNotCalledUnknown() {
        assertHints("classes/state_hint_checked_state_error", new Hint(Kind.NEEDED_STATE,
                "What is known here does not decide the state of it, and this call needs it to be hasMore"));
    }

    @Test
    void partlyKnownStateIsNamedInTheKnownStateSetsOnly() {
        assertHints("ErrorStateHintPartlyKnown.java",
                new Hint(Kind.UNREACHABLE_STATE, "No sequence of calls moves this from notRedoable to redoable"),
                new Hint(Kind.AVAILABLE_CALLS, "In state notRedoable, this can call: check()"));
    }

    @Test
    void statesDifferingInAnotherStateSetAreNamedOnce() {
        assertHints("ErrorPathDependentOtherStateSet.java",
                new Hint(Kind.CALL_SEQUENCE, "Call d.open() before this call to move d from closed to opened"),
                new Hint(Kind.AVAILABLE_CALLS, "In state closed, d can call: open()"));
    }

    @Test
    void argumentThatDecidedTheStateIsShownWithAValueThatWouldWork() {
        assertHints("ErrorArgumentPicksTheState.java", new Hint(Kind.DECIDING_ARGUMENT,
                "buffer.reset(boolean) on line 20 decides the state of buffer: with keep == true, this call would be allowed"));
    }

    @Test
    void deciderIsTheLastCallThatChangedTheCheckedStateSet() {
        assertHints("ErrorArgumentPicksTheStateEarlier.java", new Hint(Kind.DECIDING_ARGUMENT,
                "param.setTilingMode(int) on line 25 decides the state of param: with mode == 2, this call would be allowed"));
    }

    @Test
    void classWithoutStateSetKeepsTheGeneralHint() {
        assertHints("ErrorUnconstrainedStateRefinement.java", Hint.general(
                "No state information is known for this object. Make sure its state is initialized and available at this point."));
    }

    private static void assertHints(String test, Hint... expected) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream().toList();
        assertEquals(1, errors.size(), "Expected exactly one error from " + test);
        StateRefinementError error = assertInstanceOf(StateRefinementError.class, errors.get(0));
        assertEquals(List.of(expected), error.getHints());
    }
}
