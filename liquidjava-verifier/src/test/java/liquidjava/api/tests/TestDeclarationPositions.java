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
        assertDeclarations("ErrorSMTUnknown.java", "SMT Unknown Error", "_ > 2.0");
    }

    @Test
    void refinementErrorPointsToReturnPredicate() throws IOException {
        assertDeclarations("ErrorIdentity.java", "Refinement Error", "_ > 0");
    }

    @Test
    void smtUnknownPointsToStatePredicate() throws IOException {
        assertDeclarations("ErrorSMTUnknownState.java", "SMT Unknown Error", "amount(this) < limit");
    }

    @Test
    void stateErrorPointsToStatePredicate() throws IOException {
        assertDeclarations("ErrorUnconstrainedStateRefinement.java", "State Refinement Error", "ready(this)");
    }

    @Test
    void declarationsCoverReturnsParametersLocalsAndFields() throws IOException {
        assertDeclarations("ErrorRefinementDeclarationPositions.java", "Refinement Error", "_ > 20", "_ > 30", "_ > 40",
                "_ > 10");
    }

    private static void assertDeclarations(String file, String title, String... predicates) throws IOException {
        Path path = Path.of("../liquidjava-example/src/main/java/testSuite/", file).toRealPath();
        String source = Files.readString(path);
        CommandLineLauncher.launch(path.toString());
        List<LJError> errors = Diagnostics.getInstance().getErrors().stream()
                .sorted(Comparator.comparingInt(error -> error.getPosition().getSourceStart())).toList();

        assertEquals(predicates.length, errors.size());
        for (int i = 0; i < predicates.length; i++) {
            LJError error = errors.get(i);
            assertEquals(title, error.getTitle());
            assertPredicatePosition(error.getDeclarationPosition(), path, source, predicates[i]);
            assertPredicateUnderline(error, predicates[i]);
        }
    }

    private static void assertPredicatePosition(SourcePosition position, Path file, String source, String predicate)
            throws IOException {
        assertNotNull(position, "Missing refinement declaration position");
        int start = source.indexOf("\"" + predicate + "\"") + 1;
        assertTrue(start > 0, "Predicate not found in fixture: " + predicate);
        assertEquals(start, position.getSourceStart());
        assertEquals(start + predicate.length() - 1, position.getSourceEnd());
        assertEquals(file, position.getFile().toPath().toRealPath());
    }

    private static void assertPredicateUnderline(LJError error, String predicate) {
        String output = error.toString().replaceAll("\u001B\\[[;\\d]*m", "");
        String heading = "--> Refinement declared here:\n";
        assertTrue(output.contains(heading), "Missing refinement declaration snippet");
        String snippet = output.substring(output.indexOf(heading) + heading.length());
        String indent = " ".repeat(error.getDeclarationPosition().getColumn() - 1);
        String underline = "^".repeat(predicate.length());
        assertTrue(snippet.contains(indent + underline + "\n"), snippet);
    }
}
