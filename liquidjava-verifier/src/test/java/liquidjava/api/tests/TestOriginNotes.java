package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.SourceReference;
import liquidjava.diagnostics.errors.LJError;

class TestOriginNotes {

    private static final String TEST_SUITE = "../liquidjava-example/src/main/java/testSuite/";

    @Test
    void unrefinedParameterIsNamedWithItsMethod() {
        LJError error = verify("ErrorUnknownArgument.java");
        assertEquals(List.of("Origin: line 10, `amount` is a parameter of `use(int)` with no refinement"),
                error.getMessageNotes());
        assertTrue(error.toString().contains(error.getMessage() + "\n  Origin: line 10, `amount` is a parameter"));
    }

    @Test
    void stateIsTracedToTheCallThatLastSetTheSetThatIsWrong() {
        assertEquals(List.of("Origin: line 27, `d.lock()` makes `lockState(d) == locked`"),
                verify("ErrorStateHintMultipleSets.java").getMessageNotes());
    }

    @Test
    void noteNamesTheLineOfTheCallForClientsToLinkTo() {
        LJError error = verify("ErrorStateHintMultipleSets.java");
        SourceReference reference = error.getReferences().get(0);
        assertEquals(error.getMessageNotes().get(0), reference.message());
        assertEquals("line 27", reference.text());
        assertEquals("Origin: ".length(), reference.start());
        assertEquals(27, reference.line());
        assertTrue(reference.file().endsWith("ErrorStateHintMultipleSets.java"));
    }

    @Test
    void stateIsTracedToTheReturnRefinementOfTheCallAssigned() {
        assertEquals(List.of("Origin: line 27, `FromReturnedTask.create()` returns `phase(task) == idle`"),
                verify("ErrorStateFromReturnedValue.java").getMessageNotes());
    }

    @Test
    void stateOfAReceiverAnotherNameMayHoldIsNotTraced() {
        CommandLineLauncher.launch(TEST_SUITE + "ErrorDeferredCallbackState.java");
        LJError captured = Diagnostics.getInstance().getErrors().stream().filter(e -> e.getPosition().getLine() == 37)
                .findFirst().orElseThrow();
        assertEquals(List.of(), captured.getMessageNotes());
    }

    @Test
    void callArgumentIsTracedToWhatItsReturnRefinementGuaranteesAboutIt() {
        CommandLineLauncher.launch(TEST_SUITE + "classes/found_binders_error");
        LJError pick = Diagnostics.getInstance().getErrors().stream().filter(e -> e.getPosition().getLine() == 25)
                .findFirst().orElseThrow();
        assertEquals(
                List.of("Origin: ArrayListRefinements.java:13, `items.size()` guarantees only `items.size() >= 0`"),
                pick.getMessageNotes());
    }

    private static LJError verify(String test) {
        CommandLineLauncher.launch(TEST_SUITE + test);
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream().toList();
        assertEquals(1, errors.size(), "Expected exactly one error from " + test);
        return errors.get(0);
    }
}
