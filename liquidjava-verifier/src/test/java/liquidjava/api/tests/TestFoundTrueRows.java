package liquidjava.api.tests;

import static liquidjava.api.tests.FoundRows.plain;
import static liquidjava.api.tests.FoundRows.rows;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.StateRefinementError;

/**
 * Checks that Found for the errors of {@code classes/found_true_rows_error} ends with no bare true row once the binders
 * that state nothing are dropped
 */
class TestFoundTrueRows {

    private static final Map<Integer, StateRefinementError> ERRORS = new TreeMap<>();

    @BeforeAll
    static void verify() {
        CommandLineLauncher.launch("../liquidjava-example/src/main/java/testSuite/classes/found_true_rows_error");
        for (LJError error : Diagnostics.getInstance().getErrors())
            ERRORS.put(error.getPosition().getLine(), assertInstanceOf(StateRefinementError.class, error));
    }

    private static List<String> rowsAt(int line) {
        return rows(ERRORS.get(line).getFoundSimplification().getImplication());
    }

    @Test
    void foundThatSimplifiesAwayIsTrue() {
        assertEquals(List.of("true"), rowsAt(11));
        assertEquals("found true but expected compressionExplicit(wParam)", plain(ERRORS.get(11).getMessage()));
    }

    @Test
    void pathConditionIsNotFollowedByTrue() {
        assertEquals(List.of("∀#fresh baseline || extended"), rowsAt(16));
        assertEquals("found baseline || extended but expected compressionExplicit(wParam)",
                plain(ERRORS.get(16).getMessage()));
    }

    @Test
    void nestedPathConditionsAreNotFollowedByTrue() {
        assertEquals(List.of("∀#fresh #fresh == lossless && #fresh", "∀#fresh !extended"), rowsAt(23));
    }

    @Test
    void knownStateStaysAfterThePathCondition() {
        assertEquals(List.of("∀#fresh #fresh == baseline && #fresh", "∀wParam startCompression(wParam)"), rowsAt(31));
    }
}
