package liquidjava.api.tests;

import static liquidjava.utils.TestUtils.*;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.LJDiagnostic;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.utils.Pair;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TestExamples {

    Diagnostics diagnostics = Diagnostics.getInstance();

    /**
     * Runs the verifier and checks the expected diagnostics
     */
    @ParameterizedTest
    @MethodSource("sourcePaths")
    public void testPath(final Path path) {
        String pathName = path.getFileName().toString();
        boolean isDirectory = Files.isDirectory(path);

        // run verification
        CommandLineLauncher.launch(path.toFile().toString());

        List<Pair<String, Integer>> expectedWarnings = isDirectory ? getExpectedWarningsFromDirectory(path)
                : getExpectedWarningsFromFile(path);
        List<Pair<String, Integer>> expectedErrors = isDirectory ? getExpectedErrorsFromDirectory(path)
                : getExpectedErrorsFromFile(path);

        checkExpectedDiagnostics(pathName, diagnostics.getErrors(), expectedErrors, diagnostics.getErrorOutput());
        checkExpectedDiagnostics(pathName, diagnostics.getWarnings(), expectedWarnings, diagnostics.getWarningOutput());
    }

    /**
     * Checks that the found diagnostics match the expected diagnostics
     */
    private static void checkExpectedDiagnostics(String pathName, Collection<? extends LJDiagnostic> found,
            List<Pair<String, Integer>> expected, String output) {
        if (found.size() != expected.size()) {
            System.out.println("Unexpected number of diagnostics found in: " + pathName + " --- expected exactly "
                    + expected.size() + ". \n" + output);
            fail();
        }
        List<Pair<String, Integer>> unmatched = new ArrayList<>(expected);
        for (LJDiagnostic diagnostic : found) {
            int match = -1;
            for (int i = 0; i < unmatched.size(); i++) {
                if (matches(diagnostic, unmatched.get(i))) {
                    match = i;
                    break;
                }
            }
            if (match < 0) {
                System.out.println(
                        "Unexpected diagnostic in: " + pathName + " --- expected: " + expected + ". \n" + output);
                fail();
            }
            unmatched.remove(match);
        }
    }

    private static boolean matches(LJDiagnostic diagnostic, Pair<String, Integer> expected) {
        if (diagnostic.getPosition().getLine() != expected.second())
            return false;
        return !(diagnostic instanceof LJError) || diagnostic.getTitle().equals(expected.first());
    }

    /**
     * Returns the test suite paths to verify
     */
    private static Stream<Path> sourcePaths() throws IOException {
        return Files.find(Paths.get("../liquidjava-example/src/main/java/testSuite/"), Integer.MAX_VALUE,
                (filePath, fileAttr) -> {
                    String name = filePath.getFileName().toString();
                    return (fileAttr.isRegularFile() || fileAttr.isDirectory()) && isTestPath(name);
                });
    }

    private static boolean isTestPath(String path) {
        String lowerCasePath = path.toLowerCase();
        return lowerCasePath.contains("correct") || lowerCasePath.contains("error")
                || lowerCasePath.contains("warning");
    }

    /**
     * Verifies that multiple correct inputs can be processed together
     */
    @Test
    public void testMultiplePaths() {
        String[] paths = { "../liquidjava-example/src/main/java/testSuite/CorrectSimple.java",
                "../liquidjava-example/src/main/java/testSuite/classes/arraylist_correct", };
        CommandLineLauncher.launch(paths);
        // The inputs have no expected diagnostics.
        if (diagnostics.foundError() || !diagnostics.getWarnings().isEmpty()) {
            System.out.println(
                    "Unexpected diagnostic found. \n" + diagnostics.getErrorOutput() + diagnostics.getWarningOutput());
            fail();
        }
    }
}
