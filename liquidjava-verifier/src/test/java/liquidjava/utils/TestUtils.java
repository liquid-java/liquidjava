package liquidjava.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.opt.derivation_node.BinaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.DerivationNode;
import liquidjava.rj_language.opt.derivation_node.IteDerivationNode;
import liquidjava.rj_language.opt.derivation_node.UnaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import liquidjava.rj_language.opt.derivation_node.VarDerivationNode;
import spoon.Launcher;
import spoon.reflect.factory.Factory;

public class TestUtils {

    private final static Factory factory = new Launcher().getFactory();
    private final static Context context = Context.getInstance();

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

    /**
     * Helper method to compare two derivation nodes recursively
     */
    public static void assertDerivationEquals(DerivationNode expected, DerivationNode actual, String message) {
        if (expected == null && actual == null)
            return;

        assertNotNull(expected);
        assertEquals(expected.getClass(), actual.getClass(), message + ": node types should match");
        if (expected instanceof ValDerivationNode expectedVal) {
            ValDerivationNode actualVal = (ValDerivationNode) actual;
            assertEquals(expectedVal.getValue().toString(), actualVal.getValue().toString(),
                    message + ": values should match");
            assertDerivationEquals(expectedVal.getOrigin(), actualVal.getOrigin(), message + " > origin");
        } else if (expected instanceof BinaryDerivationNode expectedBin) {
            BinaryDerivationNode actualBin = (BinaryDerivationNode) actual;
            assertEquals(expectedBin.getOp(), actualBin.getOp(), message + ": operators should match");
            assertDerivationEquals(expectedBin.getLeft(), actualBin.getLeft(), message + " > left");
            assertDerivationEquals(expectedBin.getRight(), actualBin.getRight(), message + " > right");
        } else if (expected instanceof VarDerivationNode expectedVar) {
            VarDerivationNode actualVar = (VarDerivationNode) actual;
            assertEquals(expectedVar.getVar(), actualVar.getVar(), message + ": variables should match");
        } else if (expected instanceof UnaryDerivationNode expectedUnary) {
            UnaryDerivationNode actualUnary = (UnaryDerivationNode) actual;
            assertEquals(expectedUnary.getOp(), actualUnary.getOp(), message + ": operators should match");
            assertDerivationEquals(expectedUnary.getOperand(), actualUnary.getOperand(), message + " > operand");
        } else if (expected instanceof IteDerivationNode expectedIte) {
            IteDerivationNode actualIte = (IteDerivationNode) actual;
            assertDerivationEquals(expectedIte.getCondition(), actualIte.getCondition(), message + " > condition");
            assertDerivationEquals(expectedIte.getThenBranch(), actualIte.getThenBranch(), message + " > then");
            assertDerivationEquals(expectedIte.getElseBranch(), actualIte.getElseBranch(), message + " > else");
        }
    }

    /**
     * Helper method to add an integer variable to the context Needed for tests that rely on the SMT-based implication
     * checks The simplifier asks Z3 whether one conjunct implies another, so every variable in those expressions must
     * be in the context
     */
    public static void addIntVariableToContext(String name) {
        context.addVarToContext(name, factory.Type().INTEGER_PRIMITIVE, new Predicate(),
                factory.Code().createCodeSnippetStatement(""));
    }
}
