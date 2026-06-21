package liquidjava.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.UnaryOperator;

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
            assertEquals(Predicate.class, current.getRefinement().getClass(),
                    "Expected simplified refinement at implication " + i + " to be a plain Predicate");
            assertEquals(expectedPredicate.simplified(), formatRefinement(current),
                    "Unexpected simplified expression at implication " + i);
            if (expectedPredicate.origin() != null)
                assertEquals(expectedPredicate.origin(), formatOrigin(current.getOrigin()),
                        "Unexpected origin VC at implication " + i);
            current = current.getNext();
        }
        assertNull(current, "Expected VC chain to end after " + expected.length + " implications");
    }

    public static VCImplication assertSimplificationSteps(UnaryOperator<VCImplication> simplifier,
            VCImplication implication, ExpectedSimplificationStep... expectedSteps) {
        VCImplication current = implication;
        for (ExpectedSimplificationStep expectedStep : expectedSteps) {
            current = simplifier.apply(current);
            assertSimplifiedVC(current, expectedStep.implications());
        }
        return current;
    }

    public static ExpectedSimplificationStep chain(ExpectedSimplifiedVCImplication... implications) {
        return new ExpectedSimplificationStep(implications);
    }

    public static ExpectedSimplifiedVCImplication expect(String simplified, String origin) {
        return new ExpectedSimplifiedVCImplication(simplified, origin);
    }

    private static String formatOrigin(VCImplication origin) {
        if (!origin.hasBinder())
            return formatRefinement(origin);
        return "∀" + origin.getName() + ":" + origin.getType().getQualifiedName() + ". " + formatRefinement(origin);
    }

    private static String formatRefinement(VCImplication implication) {
        return implication.getRefinement().getExpression().toDisplayString();
    }

    public record ExpectedSimplifiedVCImplication(String simplified, String origin) {
    }

    public record ExpectedSimplificationStep(ExpectedSimplifiedVCImplication... implications) {
    }
}
