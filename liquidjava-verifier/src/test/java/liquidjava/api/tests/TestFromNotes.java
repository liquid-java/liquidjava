package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;

class TestFromNotes {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";

    @Test
    void unrefinedParameterIsNamedWithItsMethod() {
        LJError error = verify("ErrorUnknownArgument.java");
        assertEquals(List.of("From: amount is a parameter of use(int) (line 10) with no refinement"),
                error.getMessageNotes());
        assertTrue(error.toString().contains(error.getMessage() + "\n  From: amount is a parameter"));
    }

    private static LJError verify(String test) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream().toList();
        assertEquals(1, errors.size(), "Expected exactly one error from " + test);
        return errors.get(0);
    }
}
