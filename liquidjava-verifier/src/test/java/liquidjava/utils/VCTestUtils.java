package liquidjava.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.opt.VCSimplification;
import liquidjava.rj_language.opt.VCSimplificationPass;
import liquidjava.rj_language.opt.VCSimplificationResult;
import liquidjava.rj_language.parsing.RefinementsParser;
import spoon.Launcher;
import spoon.reflect.reference.CtTypeReference;

public class VCTestUtils {

    private static final CtTypeReference<?> INT = new Launcher().getFactory().Type().INTEGER_PRIMITIVE;

    public static VCImplication vc(String... implications) {
        VCImplication first = null;
        VCImplication last = null;
        for (String implication : implications) {
            VCImplication node = parseImplication(implication);
            if (first == null)
                first = node;
            if (last != null)
                last.setNext(node);
            last = node;
        }
        return first;
    }

    public static VCSimplificationResult assertSimplificationSteps(VCImplication implication,
            ExpectedSimplificationStep... expectedSteps) {
        VCSimplificationResult current = new VCSimplificationResult(implication);
        for (ExpectedSimplificationStep expectedStep : expectedSteps) {
            VCSimplificationResult result = VCSimplification.simplifyOnce(current);
            assertSimplificationResult(current, result, expectedStep);
            current = result;
        }
        return current;
    }

    public static VCSimplificationResult assertSimplificationSteps(VCSimplificationPass simplifier,
            VCImplication implication, ExpectedSimplificationStep... expectedSteps) {
        VCSimplificationResult current = new VCSimplificationResult(implication);
        for (ExpectedSimplificationStep expectedStep : expectedSteps) {
            VCSimplificationResult result = VCSimplification.simplifyOnce(current, simplifier);
            assertSimplificationResult(current, result, expectedStep);
            current = result;
        }
        return current;
    }

    private static void assertSimplificationResult(VCSimplificationResult previous, VCSimplificationResult result,
            ExpectedSimplificationStep expectedStep) {
        if (previous.getImplication().equals(result.getImplication())) {
            assertNull(result.getOrigin(), "Unchanged simplification result should not have an origin");
        } else {
            assertNotNull(result.getOrigin(), "Changed simplification result should have an origin");
            assertNotSame(result, result.getOrigin(), "Simplification result should not be its own origin");
            assertNotEquals(result.getImplication(), result.getOrigin().getImplication(),
                    "Simplification origin should differ from the simplified VC");
            assertEquals(previous.getImplication(), result.getOrigin().getImplication(),
                    "Simplification origin should be the complete previous VC");
        }
        assertSimplifiedVC(result.getImplication(), expectedStep.implications());
    }

    private static void assertSimplifiedVC(VCImplication implication, String... expected) {
        VCImplication current = implication;
        for (int i = 0; i < expected.length; i++) {
            assertNotNull(current, "Expected implication " + i + " with refinement " + expected[i]);
            assertEquals(Predicate.class, current.getRefinement().getClass(),
                    "Expected simplified refinement at implication " + i + " to be a plain Predicate");
            assertEquals(expected[i], formatRefinement(current),
                    "Unexpected simplified expression at implication " + i);
            current = current.getNext();
        }
        assertNull(current, "Expected VC chain to end after " + expected.length + " implications");
    }

    private static VCImplication parseImplication(String implication) {
        if (!implication.startsWith("∀"))
            return new VCImplication(new Predicate(RefinementsParser.createAST(implication, "")));

        int refinementStart = implication.indexOf('.');
        String binder = implication.substring(1, refinementStart).trim();
        String refinement = implication.substring(refinementStart + 1).trim();
        String[] parts = binder.split(":");
        return new VCImplication(parts[0].trim(), type(parts[1].trim()),
                new Predicate(RefinementsParser.createAST(refinement, "")));
    }

    private static CtTypeReference<?> type(String name) {
        if ("int".equals(name))
            return INT;
        throw new IllegalArgumentException("Unsupported test type: " + name);
    }

    private static String formatRefinement(VCImplication implication) {
        return implication.getRefinement().getExpression().toDisplayString();
    }

    public static ExpectedSimplificationStep step(String... implications) {
        return new ExpectedSimplificationStep(implications);
    }

    public record ExpectedSimplificationStep(String... implications) {
    }
}
