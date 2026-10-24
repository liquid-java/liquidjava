package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.StateErrorContext;
import liquidjava.diagnostics.errors.StateRefinementError;

class TestStateErrorContext {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";

    @Test
    void callInTheWrongStateHasThePossibleAndAcceptedStates() {
        assertEquals(
                new StateErrorContext("f", "testSuite.ErrorStateHintCallSequence", "read()",
                        List.of(List.of("created")), List.of(List.of("open")), true),
                contextOf("ErrorStateHintCallSequence.java"));
    }

    @Test
    void statesAreNamedInEachStateSet() {
        assertEquals(
                new StateErrorContext("d", "testSuite.ErrorStateHintMultipleSets", "open()",
                        List.of(List.of("closed", "locked")), List.of(List.of("closed", "unlocked")), true),
                contextOf("ErrorStateHintMultipleSets.java"));
    }

    @Test
    void stateDependingOnThePathHasEveryPossibleState() {
        assertEquals(List.of(List.of("created"), List.of("open")),
                contextOf("ErrorStateHintPathDependent.java").possibleStates());
    }

    @Test
    void transitionDependingOnArgumentsLeavesTheAcceptedStatesUnknown() {
        StateErrorContext context = contextOf("ErrorStateContextArgumentDependent.java");
        assertNull(context.acceptedStates());
        assertFalse(context.complete());
    }

    @Test
    void classWithoutStateSetHasNoContext() {
        assertNull(contextOf("ErrorUnconstrainedStateRefinement.java"));
    }

    private static StateErrorContext contextOf(String test) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream().toList();
        assertEquals(1, errors.size(), "Expected exactly one error from " + test);
        return assertInstanceOf(StateRefinementError.class, errors.get(0)).getContext();
    }
}
