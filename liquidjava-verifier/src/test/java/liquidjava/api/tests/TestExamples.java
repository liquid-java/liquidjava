package liquidjava.api.tests;

import static liquidjava.utils.TestUtils.*;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
     * Test the file at the given path by launching the verifier and checking for errors and warnings. The
     * file/directory is expected to be either correct, contain an error, or report warnings based on its name.
     *
     * @param path
     *            path to the file to test
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

        if (shouldWarn(pathName)) {
            checkExpectedDiagnostics(pathName, diagnostics.getWarnings(), expectedWarnings,
                    diagnostics.getWarningOutput());
        }

        // verification should pass, check if any errors were found
        if (shouldPass(pathName) && diagnostics.foundError()) {
            System.out.println("Error in: " + pathName + " --- should pass but an error was found. \n"
                    + diagnostics.getErrorOutput());
            fail();
        }
        // verification should fail, check if it failed as expected (multiple errors can be found)
        else if (shouldFail(pathName)) {
            if (!diagnostics.foundError()) {
                System.out.println("Error in: " + pathName + " --- should fail but no errors were found. \n"
                        + diagnostics.getErrorOutput());
                fail();
            } else {
                // check if expected error was found
                List<Pair<String, Integer>> expectedErrors = isDirectory ? getExpectedErrorsFromDirectory(path)
                        : getExpectedErrorsFromFile(path);
                checkExpectedDiagnostics(pathName, diagnostics.getErrors(), expectedErrors,
                        diagnostics.getErrorOutput());
            }
        }
    }

    private static void checkExpectedDiagnostics(String pathName, Collection<? extends LJDiagnostic> found,
            List<Pair<String, Integer>> expected, String output) {
        if (found.size() != expected.size()) {
            System.out.println("Unexpected number of diagnostics found in: " + pathName + " --- expected exactly "
                    + expected.size() + ". \n" + output);
            fail();
        }
        if (expected.isEmpty()) {
            System.out.println("No expected diagnostic messages found for: " + pathName);
            System.out.println(
                    "Please specify each expected diagnostic in the test file as a comment on the line where it should be reported.");
            fail();
        }
        for (LJDiagnostic diagnostic : found) {
            boolean match = expected.stream().anyMatch(expectedDiagnostic -> matches(diagnostic, expectedDiagnostic));
            if (!match) {
                System.out.println(
                        "Unexpected diagnostic in: " + pathName + " --- expected: " + expected + ". \n" + output);
                fail();
            }
        }
    }

    private static boolean matches(LJDiagnostic diagnostic, Pair<String, Integer> expected) {
        if (diagnostic.getPosition().getLine() != expected.second())
            return false;
        return !(diagnostic instanceof LJError) || diagnostic.getTitle().equals(expected.first());
    }

    /**
     * Returns a Stream of paths to test files in the testSuite directory. These include files with names starting with
     * "Correct" or "Error", and directories containing "correct" or "error". §
     * 
     * @return Stream of paths to test files
     *
     * @throws IOException
     *             if an I/O error occurs or the path does not exist
     */
    private static Stream<Path> sourcePaths() throws IOException {
        return Files.find(Paths.get("../liquidjava-example/src/main/java/testSuite/"), Integer.MAX_VALUE,
                (filePath, fileAttr) -> {
                    String name = filePath.getFileName().toString();
                    // Files that start with "Correct", "Error" or "Warning"
                    boolean isFileStartingWithCorrectOrError = fileAttr.isRegularFile()
                            && (shouldPass(name) || shouldFail(name) || shouldWarn(name));

                    // Directories that contain "correct", "error" or "warning"
                    boolean isDirectoryWithCorrectOrError = fileAttr.isDirectory()
                            && (shouldPass(name) || shouldFail(name) || shouldWarn(name));

                    // Return true if either condition matches
                    return isFileStartingWithCorrectOrError || isDirectoryWithCorrectOrError;
                });
    }

    /**
     * Test multiple paths at once, including both files and directories. This test ensures that the verifier can handle
     * multiple inputs correctly and that no errors are found in files/directories that are expected to be correct.
     */
    @Test
    public void testMultiplePaths() {
        String[] paths = { "../liquidjava-example/src/main/java/testSuite/CorrectSimple.java",
                "../liquidjava-example/src/main/java/testSuite/classes/arraylist_correct", };
        CommandLineLauncher.launch(paths);
        // Check if any of the paths that should be correct found an error
        if (diagnostics.foundError()) {
            System.out.println("Error found in files that should be correct. \n" + diagnostics.getErrorOutput());
            fail();
        }
    }
}
