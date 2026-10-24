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
    void unknownStateListsTheCallsThatLeadToTheExpectedState() {
        assertHints("ErrorStateHintUnknownState.java", new Hint(Kind.REACHING_CALLS,
                "The state of f is unknown here. To get it to open, use: f.open() (from created)"));
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
