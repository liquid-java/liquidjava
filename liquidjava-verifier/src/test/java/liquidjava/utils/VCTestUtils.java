package liquidjava.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.SimplifiedPredicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.parsing.RefinementsParser;
import spoon.Launcher;
import spoon.reflect.reference.CtTypeReference;

public class VCTestUtils {

    private static final CtTypeReference<?> INT = new Launcher().getFactory().Type().INTEGER_PRIMITIVE;

    public static Expression parse(String refinement) {
        return RefinementsParser.createAST(refinement, "");
    }

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
            return new VCImplication(new Predicate(parse(implication)));

        int refinementStart = implication.indexOf('.');
        String binder = implication.substring(1, refinementStart).trim();
        String refinement = implication.substring(refinementStart + 1).trim();
        String[] parts = binder.split(":");
        return new VCImplication(parts[0].trim(), type(parts[1].trim()), new Predicate(parse(refinement)));
    }

    private static CtTypeReference<?> type(String name) {
        if ("int".equals(name))
            return INT;
        throw new IllegalArgumentException("Unsupported test type: " + name);
    }

    public static void assertSimplifiedVC(VCImplication implication, String... expected) {
        ExpectedSimplifiedPredicate[] predicates = java.util.Arrays.stream(expected)
                .map(VCTestUtils::parseExpectedSimplifiedPredicate).toArray(ExpectedSimplifiedPredicate[]::new);
        assertSimplifiedVC(implication, predicates);
    }

    public static void assertSimplifiedVC(VCImplication implication, ExpectedSimplifiedPredicate... expected) {
        VCImplication current = implication;
        for (int i = 0; i < expected.length; i++) {
            ExpectedSimplifiedPredicate expectedPredicate = expected[i];
            SimplifiedPredicate predicate = simplifiedPredicate(current, i);
            assertEquals(expectedPredicate.simplified(), predicate.getSimplifiedPredicate().toString(),
                    "Unexpected simplified expression at implication " + i);
            if (expectedPredicate.origin() != null)
                assertEquals(expectedPredicate.origin(), predicate.getOrigin().toString(),
                        "Unexpected origin expression at implication " + i);
            if (expectedPredicate.binders() != null)
                assertEquals(expectedPredicate.binders(), formatBinders(predicate),
                        "Unexpected binders at implication " + i);
            current = current.getNext();
        }
        assertNull(current, "Expected VC chain to end after " + expected.length + " implications");
    }

    public static ExpectedSimplifiedPredicate simplified(String simplified) {
        return new ExpectedSimplifiedPredicate(simplified, null, null);
    }

    public static ExpectedSimplifiedPredicate simplified(String simplified, String origin) {
        return new ExpectedSimplifiedPredicate(simplified, origin, null);
    }

    public static ExpectedSimplifiedPredicate simplified(String simplified, String origin, String binders) {
        return new ExpectedSimplifiedPredicate(simplified, origin, binders);
    }

    public static void assertVC(VCImplication implication, String... expected) {
        VCImplication current = implication;
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], current.getRefinement().getExpression().toString(),
                    "Unexpected expression at implication " + i);
            current = current.getNext();
        }
        assertNull(current, "Expected VC chain to end after " + expected.length + " implications");
    }

    public static SimplifiedPredicate simplifiedPredicate(VCImplication implication, int index) {
        assertInstanceOf(SimplifiedPredicate.class, implication.getRefinement(),
                "Expected implication " + index + " to contain a SimplifiedPredicate");
        return (SimplifiedPredicate) implication.getRefinement();
    }

    private static String formatBinders(SimplifiedPredicate predicate) {
        return predicate.getBinders().stream().map(binder -> binder.getName() + ":" + binder.getType())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static ExpectedSimplifiedPredicate parseExpectedSimplifiedPredicate(String expected) {
        String binders = null;
        String expression = expected.trim();
        int binderStart = expression.lastIndexOf('[');
        if (binderStart >= 0) {
            int binderEnd = expression.lastIndexOf(']');
            binders = expression.substring(binderStart + 1, binderEnd).trim();
            expression = expression.substring(0, binderStart).trim();
        }

        String[] parts = expression.split("<-", 2);
        String simplified = parts[0].trim();
        String origin = parts.length > 1 ? parts[1].trim() : null;
        return new ExpectedSimplifiedPredicate(simplified, origin, binders);
    }

    public record ExpectedSimplifiedPredicate(String simplified, String origin, String binders) {
    }
}
