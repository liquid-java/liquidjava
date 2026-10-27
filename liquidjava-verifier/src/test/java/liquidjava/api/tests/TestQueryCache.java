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

    @TempDir
    Path dir;

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
        assertSameAsUncachedAfterEdit(String.format(BOX, "int"), String.format(BOX, "long"));
    }

    /** Verifies {@code before}, then {@code after} with the cache, which must report what it reports without it */
    private void assertSameAsUncachedAfterEdit(String before, String after) throws IOException {
        List<String> reportedBefore = verify(before, CacheMode.ON);
        List<String> cached = verify(after, CacheMode.ON);
        List<String> uncached = verify(after, CacheMode.OFF);
        assertEquals(uncached, cached);
        assertNotEquals(reportedBefore, uncached, "the edit must change what the verifier reports");
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
