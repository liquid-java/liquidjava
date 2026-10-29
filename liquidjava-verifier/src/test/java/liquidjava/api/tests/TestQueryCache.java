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
import liquidjava.diagnostics.errors.NotFoundError;
import liquidjava.diagnostics.errors.SyntaxError;
import liquidjava.utils.CacheMode;

/**
 * The cache that keeps solver results across verification runs must never change what the verifier reports. Each test
 * verifies a program, edits one thing the solver queries depend on without changing their text, and verifies it again,
 * as the language server does after a save: the result must be the same as without the cache, and must change.
 */
class TestQueryCache {

    private static final String DOOR = """
            import liquidjava.specification.StateRefinement;
            import liquidjava.specification.StateSet;

            @StateSet({"created", "open", "closed"})
            public class Test {

                @StateRefinement(to = "created(this)")
                public Test() {}

                @StateRefinement(from = "created(this)", to = "%s(this)")
                public void open() {}

                @StateRefinement(from = "open(this)")
                public void read() {}

                @StateRefinement(from = "%s", to = "closed(this)")
                public void close() {}

                public static void main(String[] args) {
                    Test d = new Test();
                    d.open();
                    d.read();
                }
            }
            """;

    private static final String NUMBER = """
            import liquidjava.specification.Refinement;

            public class Test {
                public static void atLeastOne(@Refinement("v >= 1") %1$s v) {}

                public static void positive(@Refinement("v > 0") %1$s v) {
                    atLeastOne(v);
                }
            }
            """;

    private static final String COLOR = """
            import liquidjava.specification.Refinement;

            public class Test {
                enum Color { %s }

                public static void green(@Refinement("c == Color.Green") Color c) {}

                public static void notRed(@Refinement("c != Color.Red") Color c) {
                    green(c);
                }
            }
            """;

    private static final String BOX = """
            import liquidjava.specification.Ghost;
            import liquidjava.specification.StateRefinement;

            @Ghost("%s size")
            public class Test {

                @StateRefinement(to = "size(this) > 0")
                public Test() {}

                @StateRefinement(from = "size(this) >= 1")
                public void take() {}

                public static void main(String[] args) {
                    Test b = new Test();
                    b.take();
                }
            }
            """;

    private static final String MODE = """
            import liquidjava.specification.Refinement;

            public class Test {
                enum Mode { Photo, Video }

                public static void photo(@Refinement("m == Mode.Photo") Mode m) {}

                public static void main(String[] args) {
                    photo(Mode.Photo);
                    photo(Mode.Video);
                }
            }
            """;

    @TempDir
    Path dir;

    @Test
    void enumConstantsAreValuesNotVariables() throws IOException {
        // the two calls only differ in the enum constant: the second must not reuse the result of the first
        List<String> cached = verify(MODE, CacheMode.ON);
        assertEquals(verify(MODE, CacheMode.OFF), cached);
        assertEquals(1, Diagnostics.getInstance().getErrors().size(), "photo(Mode.Video) must be reported");
    }

    @Test
    void editedTransition() throws IOException {
        assertSameAsUncachedAfterEdit(String.format(DOOR, "open", "open(this)"),
                String.format(DOOR, "closed", "closed(this)"));
    }

    @Test
    void stateRefinementThatCanNeverHold() throws IOException {
        assertSameAsUncachedAfterEdit(String.format(DOOR, "open", "open(this)"),
                String.format(DOOR, "open", "open(this) && closed(this)"));
    }

    @Test
    void typeOfAVariable() throws IOException {
        // integers greater than 0 are at least 1, but long values are reals to the solver
        assertSameAsUncachedAfterEdit(String.format(NUMBER, "int"), String.format(NUMBER, "long"));
    }

    @Test
    void constantsOfAnEnum() throws IOException {
        assertSameAsUncachedAfterEdit(String.format(COLOR, "Red, Green"), String.format(COLOR, "Red, Green, Blue"));
    }

    @Test
    void typeOfAGhost() throws IOException {
        // sizes greater than 0 are at least 1 for an int ghost, but not for a double one
        assertSameAsUncachedAfterEdit(String.format(BOX, "int"), String.format(BOX, "double"));
    }

    /** Verifies {@code before}, then {@code after} with the cache, which must report what it reports without it */
    private void assertSameAsUncachedAfterEdit(String before, String after) throws IOException {
        List<String> reportedBefore = verify(before, CacheMode.ON);
        assertWellFormed(before);
        List<String> cached = verify(after, CacheMode.ON);
        List<String> uncached = verify(after, CacheMode.OFF);
        assertWellFormed(after);
        assertEquals(uncached, cached);
        assertNotEquals(reportedBefore, uncached, "the edit must change what the verifier reports");
    }

    /** The program must reach the solver, or the test would not exercise the cache */
    private static void assertWellFormed(String source) {
        assertTrue(
                Diagnostics.getInstance().getErrors().stream()
                        .noneMatch(e -> e instanceof SyntaxError || e instanceof NotFoundError),
                () -> "not well formed:\n" + source);
    }

    private List<String> verify(String source, CacheMode mode) throws IOException {
        Files.writeString(dir.resolve("Test.java"), source);
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
