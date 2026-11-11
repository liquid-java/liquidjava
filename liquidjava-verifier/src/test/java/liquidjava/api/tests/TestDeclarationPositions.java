package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import spoon.reflect.cu.SourcePosition;

class TestDeclarationPositions {
    @Test
    void smtUnknownPointsToReturnPredicate() throws IOException {
        assertDeclarations("ErrorSMTUnknown.java", "_ > 2.0");
    }

    @Test
    void refinementErrorPointsToReturnPredicate() throws IOException {
        assertDeclarations("ErrorIdentity.java", "_ > 0");
    }

    @Test
    void smtUnknownPointsToStatePredicate() throws IOException {
        assertDeclarations("ErrorSMTUnknownState.java", "amount(this) < limit");
    }

    @Test
    void stateErrorPointsToStatePredicate() throws IOException {
        assertDeclarations("ErrorUnconstrainedStateRefinement.java", "ready(this)");
    }

    @Test
    void declarationsCoverReturnsParametersLocalsAndFields() throws IOException {
        assertDeclarations("ErrorRefinementDeclarationPositions.java", "_ > 20", "_ > 30", "_ > 40", "_ > 10");
    }

    @Test
    void stateErrorPointsToEveryEntryStateWithWhatItMeans() throws IOException {
        Path path = Path.of("../liquidjava-example/src/main/java/testSuite/ErrorStateRefinementEveryFrom.java")
                .toRealPath();
        String source = Files.readString(path);
        CommandLineLauncher.launch(path.toString());
        List<LJError> errors = List.copyOf(Diagnostics.getInstance().getErrors());

        assertEquals(1, errors.size());
        List<SourcePosition> positions = errors.get(0).getDeclarationPositions();
        assertEquals(2, positions.size());
        assertPosition(positions.get(0), path, fromStart(source, "shut(this)"), "shut(this)");
        // the second annotation's from, not the same text the first writes as its to
        assertPosition(positions.get(1), path, fromStart(source, "ajar(this)"), "ajar(this)");
        String notes = "  where `shut(this)` means `door(this) == shut`\n  where `ajar(this)` means `door(this) == ajar`\n";
        assertTrue(errors.get(0).toString().contains(notes));
    }

    private static int fromStart(String source, String from) {
        String written = "from = \"";
        return source.indexOf(written + from + "\"") + written.length();
    }

    private static void assertDeclarations(String file, String... predicates) throws IOException {
        Path path = Path.of("../liquidjava-example/src/main/java/testSuite/", file).toRealPath();
        String source = Files.readString(path);
        CommandLineLauncher.launch(path.toString());
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream()
                .sorted(Comparator.comparingInt(error -> error.getPosition().getSourceStart())).toList();

        assertEquals(predicates.length, errors.size());
        for (int i = 0; i < predicates.length; i++) {
            LJError error = errors.get(i);
            assertPredicatePosition(error.getDeclarationPosition(), path, source, predicates[i]);
        }
    }

    private static void assertPredicatePosition(SourcePosition position, Path file, String source, String predicate)
            throws IOException {
        assertNotNull(position, "Missing refinement declaration position");
        assertTrue(position.isValidPosition(), "Invalid refinement declaration position");
        int start = source.indexOf("\"" + predicate + "\"") + 1;
        assertTrue(start > 0, "Predicate not found in fixture: " + predicate);
        assertPosition(position, file, start, predicate);
    }

    private static void assertPosition(SourcePosition position, Path file, int start, String predicate)
            throws IOException {
        assertEquals(start, position.getSourceStart());
        assertEquals(start + predicate.length() - 1, position.getSourceEnd());
        assertEquals(file, position.getFile().toPath().toRealPath());
    }

}
