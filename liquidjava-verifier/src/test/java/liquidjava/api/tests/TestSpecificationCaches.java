package liquidjava.api.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.LJDiagnostic;
import liquidjava.diagnostics.errors.StateConflictError;
import liquidjava.utils.CacheMode;

/**
 * The cache that keeps solver results across verification runs must never change what the verifier reports, in
 * particular after a specification is edited between runs, as in the language server
 */
class TestSpecificationCaches {

    private static final String DOOR = """
            import liquidjava.specification.StateRefinement;
            import liquidjava.specification.StateSet;

            @StateSet({"created", "open", "closed"})
            public class Door {

                @StateRefinement(to = "created(this)")
                public Door() {}

                @StateRefinement(from = "created(this)", to = "%s(this)")
                public void open() {}

                @StateRefinement(from = "open(this)")
                public void read() {}

                @StateRefinement(from = "%s", to = "closed(this)")
                public void close() {}

                public static void main(String[] args) {
                    Door d = new Door();
                    d.read();
                }
            }
            """;

    @TempDir
    Path dir;

    @Test
    void editedTransitionIsNotTakenFromTheCache() throws IOException {
        verifyCached(door("open", "open(this)"));
        List<String> edited = verifyCached(door("closed", "closed(this)"));
        assertEquals(verifyUncached(), edited);
    }

    @Test
    void stateRefinementThatCanNeverHoldIsReportedAfterAnEdit() throws IOException {
        verifyCached(door("open", "open(this)"));
        List<String> edited = verifyCached(door("open", "open(this) && closed(this)"));
        assertEquals(verifyUncached(), edited);
        assertTrue(Diagnostics.getInstance().getErrors().stream().anyMatch(e -> e instanceof StateConflictError),
                "the conflicting state refinement must be reported");
    }

    private String door(String opened, String closable) {
        return String.format(DOOR, opened, closable);
    }

    /** Verifies {@code source} with the caches in use, and returns what the verifier reported */
    private List<String> verifyCached(String source) throws IOException {
        Files.writeString(dir.resolve("Door.java"), source);
        return verify(CacheMode.ON);
    }

    private List<String> verifyUncached() {
        return verify(CacheMode.OFF);
    }

    private List<String> verify(CacheMode mode) {
        String previous = System.getProperty(CacheMode.PROPERTY);
        System.setProperty(CacheMode.PROPERTY, mode.name());
        try {
            CommandLineLauncher.launch(dir.toString());
        } finally {
            if (previous == null)
                System.clearProperty(CacheMode.PROPERTY);
            else
                System.setProperty(CacheMode.PROPERTY, previous);
        }
        Diagnostics diagnostics = Diagnostics.getInstance();
        return Stream.concat(diagnostics.getErrors().stream(), diagnostics.getWarnings().stream())
                .map(LJDiagnostic::toString).toList();
    }
}
