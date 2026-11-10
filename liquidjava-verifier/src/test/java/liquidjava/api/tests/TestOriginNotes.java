package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;

class TestOriginNotes {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";

    @Test
    void unrefinedParameterIsNamedWithItsMethod() {
        LJError error = verify("ErrorUnknownArgument.java");
        assertEquals(List.of("Origin: line 10, amount is a parameter of use(int) with no refinement"),
                error.getMessageNotes());
        assertTrue(error.toString().contains(error.getMessage() + "\n  Origin: line 10, amount is a parameter"));
    }

    @Test
    void stateIsTracedToTheCallThatLastSetTheSetThatIsWrong() {
        assertEquals(List.of("From: lockState(d) == locked, last set at line 27: d.lock()"),
                verify("ErrorStateHintMultipleSets.java").getMessageNotes());
    }

    @Test
    void stateIsTracedToTheReturnRefinementOfTheCallAssigned() {
        assertEquals(List.of("From: task = FromReturnedTask.create(), line 27, which returns phase(task) == idle"),
                verify("ErrorStateFromReturnedValue.java").getMessageNotes());
    }

    private static LJError verify(String test) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream().toList();
        assertEquals(1, errors.size(), "Expected exactly one error from " + test);
        return errors.get(0);
    }
}
