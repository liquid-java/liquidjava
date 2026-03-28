package liquidjava.api.tests;

import static liquidjava.utils.TestUtils.*;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.*;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TestExamples {

    Diagnostics diagnostics = Diagnostics.getInstance();

    /**
     * Test the file at the given path by launching the verifier and checking for errors. The file/directory is expected
     * to be either correct or contain an error based on its name.
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
        CommandLineLauncher.launch(path.toAbsolutePath().toString());

        // verification should pass, check if any errors were found
        if (shouldPass(pathName) && diagnostics.foundError()) {
            System.out.println("Error in: " + pathName + " --- should pass but an error was found");
            fail();
        }
        // verification should fail, check if it failed as expected (we assume each error test has exactly one error)
        else if (shouldFail(pathName)) {
            if (!diagnostics.foundError()) {
                System.out.println("Error in: " + pathName + " --- should fail but no errors were found");
                fail();
            } else {
                // check if expected error was found
                Optional<String> expected = isDirectory ? getExpectedErrorFromDirectory(path)
                        : getExpectedErrorFromFile(path);
                if (diagnostics.getErrors().size() > 1) {
                    System.out.println("Multiple errors found in: " + pathName + " --- expected exactly one error");
                    fail();
                }
                LJError error = diagnostics.getErrors().iterator().next();
                if (error.getPosition() == null) {
                    System.out.println("Error in: " + pathName + " --- error has no position information");
                    fail();
                }
                if (expected.isPresent()) {
                    String expectedError = expected.get();
                    String foundError = error.getTitle();
                    if (!foundError.equalsIgnoreCase(expectedError)) {
                        System.out.println("Error in: " + pathName + " --- expected error: " + expectedError
                                + ", but found: " + foundError);
                        fail();
                    }
                } else {
                    System.out.println("No expected error message found for: " + pathName);
                    System.out.println("Please provide an expected error in " + (isDirectory
                            ? "a .expected file in the directory" : "the first line of the test file as a comment"));
                    fail();
                }
            }
        }
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
                    // Files that start with "Correct" or "Error"
                    boolean isFileStartingWithCorrectOrError = fileAttr.isRegularFile()
                            && (shouldPass(name) || shouldFail(name));

                    // Directories that contain "correct" or "error"
                    boolean isDirectoryWithCorrectOrError = fileAttr.isDirectory()
                            && (shouldPass(name) || shouldFail(name));

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
            System.out.println("Error found in files that should be correct");
            fail();
        }
    }
}
