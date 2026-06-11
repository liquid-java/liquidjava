package liquidjava.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.UnaryOperator;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
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

    public static void assertSimplifiedVC(VCImplication implication, ExpectedSimplifiedVCImplication... expected) {
        VCImplication current = implication;
        for (int i = 0; i < expected.length; i++) {
            ExpectedSimplifiedVCImplication expectedPredicate = expected[i];
            SimplifiedVCImplication simplified = simplifiedImplication(current, i);
            assertEquals(Predicate.class, simplified.getRefinement().getClass(),
                    "Expected simplified refinement at implication " + i + " to be a plain Predicate");
            assertEquals(expectedPredicate.simplified(), simplified.getRefinement().toString(),
                    "Unexpected simplified expression at implication " + i);
            if (expectedPredicate.origin() != null)
                assertEquals(expectedPredicate.origin(), formatOrigin(simplified.getOrigin()),
                        "Unexpected origin VC at implication " + i);
            current = current.getNext();
        }
        assertNull(current, "Expected VC chain to end after " + expected.length + " implications");
    }

    public static VCImplication assertSimplificationSteps(UnaryOperator<VCImplication> simplifier,
            VCImplication implication, ExpectedSimplifiedVCImplication... expectedSteps) {
        VCImplication current = implication;
        for (ExpectedSimplifiedVCImplication expectedStep : expectedSteps) {
            current = simplifier.apply(current);
            assertSimplifiedVC(current, expectedStep);
        }
        return current;
    }

    public static VCImplication assertSimplificationSteps(UnaryOperator<VCImplication> simplifier,
            VCImplication implication, String... expectedSteps) {
        VCImplication current = implication;
        for (int i = 0; i < expectedSteps.length; i++) {
            current = simplifier.apply(current);
            assertExpectedVCChain(current, expectedSteps[i], i);
        }
        return current;
    }

    public static SimplifiedVCImplication simplifiedImplication(VCImplication implication, int index) {
        return assertInstanceOf(SimplifiedVCImplication.class, implication,
                "Expected implication " + index + " to be a SimplifiedVCImplication");
    }

    private static void assertExpectedVCChain(VCImplication implication, String expectedStep, int step) {
        VCImplication current = implication;
        String[] expected = expectedStep.trim().split("\\s*->\\s*");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], formatImplication(current),
                    "Unexpected expression at simplification step " + step + ", implication " + i);
            current = current.getNext();
        }
        assertNull(current,
                "Expected simplification step " + step + " to end after " + expected.length + " implications");
    }

    private static String formatOrigin(VCImplication origin) {
        if (!origin.hasBinder())
            return origin.getRefinement().toString();
        return "∀" + origin.getName() + ":" + origin.getType().getQualifiedName() + ". " + origin.getRefinement();
    }

    private static String formatImplication(VCImplication implication) {
        if (!implication.hasBinder())
            return implication.getRefinement().toString();
        return "∀" + implication.getName() + ":" + implication.getType().getQualifiedName() + ". "
                + implication.getRefinement();
    }

    public record ExpectedSimplifiedVCImplication(String simplified, String origin) {
    }
}
