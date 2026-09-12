package liquidjava.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import spoon.Launcher;
import spoon.reflect.factory.Factory;

public class TestUtils {

    private static final Pattern EXPECTED_DIAGNOSTIC = Pattern.compile("//\\s*Expect:\\s*(.*?\\b(Error|Warning)\\b)",
            Pattern.CASE_INSENSITIVE);
    private final static Factory factory = new Launcher().getFactory();
    private final static Context context = Context.getInstance();

    public static boolean shouldPass(String path) {
        return path.toLowerCase().contains("correct");
    }

    public static boolean shouldFail(String path) {
        return path.toLowerCase().contains("error");
    }

    public static boolean shouldWarn(String path) {
        return path.toLowerCase().contains("warning");
    }

    public static List<Pair<String, Integer>> getExpectedErrorsFromFile(Path filePath) {
        return getExpectedDiagnosticsFromFile(filePath, "error");
    }

    private static List<Pair<String, Integer>> getExpectedDiagnosticsFromFile(Path filePath, String type) {
        List<Pair<String, Integer>> expectedDiagnostics = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                Matcher matcher = EXPECTED_DIAGNOSTIC.matcher(line);
                if (matcher.find() && matcher.group(2).equalsIgnoreCase(type)) {
                    expectedDiagnostics.add(new Pair<>(matcher.group(1).trim(), lineNumber));
                }
            }
        } catch (IOException e) {
            return List.of();
        }
        return expectedDiagnostics;
    }

    public static List<Pair<String, Integer>> getExpectedWarningsFromFile(Path filePath) {
        return getExpectedDiagnosticsFromFile(filePath, "warning");
    }

    public static List<Pair<String, Integer>> getExpectedErrorsFromDirectory(Path dirPath) {
        return getExpectedDiagnosticsFromDirectory(dirPath, "error");
    }

    public static List<Pair<String, Integer>> getExpectedWarningsFromDirectory(Path dirPath) {
        return getExpectedDiagnosticsFromDirectory(dirPath, "warning");
    }

    private static List<Pair<String, Integer>> getExpectedDiagnosticsFromDirectory(Path dirPath, String type) {
        List<Pair<String, Integer>> expectedDiagnostics = new ArrayList<>();
        try {
            List<Path> files = Files.list(dirPath).filter(Files::isRegularFile).toList();
            for (Path file : files) {
                expectedDiagnostics.addAll(getExpectedDiagnosticsFromFile(file, type));
            }
        } catch (IOException e) {
            return List.of();
        }
        return expectedDiagnostics;
    }

    public static void addIntVariableToContext(String name) {
        context.addVarToContext(name, factory.Type().INTEGER_PRIMITIVE, new Predicate(), factory.Code().createCodeSnippetStatement(""));
    }
}
