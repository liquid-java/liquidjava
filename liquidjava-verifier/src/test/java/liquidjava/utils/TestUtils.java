package liquidjava.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class TestUtils {

    /**
     * Determines if the given path indicates that the test should pass
     * 
     * @param path
     */
    public static boolean shouldPass(String path) {
        return path.toLowerCase().contains("correct") || path.toLowerCase().contains("warning");
    }

    /**
     * Determines if the given path indicates that the test should fail
     * 
     * @param path
     */
    public static boolean shouldFail(String path) {
        return path.toLowerCase().contains("error");
    }

    /**
     * Reads the expected error message from the first line of the given test file
     * 
     * @param filePath
     * 
     * @return optional containing the expected error message if present, otherwise empty
     */
    public static Optional<String> getExpectedErrorFromFile(Path filePath) {
        try (Stream<String> lines = Files.lines(filePath)) {
            Optional<String> first = lines.findFirst();
            if (first.isPresent() && first.get().startsWith("//")) {
                return Optional.of(first.get().substring(2).trim());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Reads the expected error message from a .expected file in the given directory
     * 
     * @param dirPath
     * 
     * @return optional containing the expected error message if present, otherwise empty
     */
    public static Optional<String> getExpectedErrorFromDirectory(Path dirPath) {
        Path expectedFilePath = dirPath.resolve(".expected");
        if (Files.exists(expectedFilePath)) {
            try (Stream<String> lines = Files.lines(expectedFilePath)) {
                return lines.findFirst().map(String::trim);
            } catch (IOException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
