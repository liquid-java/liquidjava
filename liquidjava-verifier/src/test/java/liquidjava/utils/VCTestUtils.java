package liquidjava.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.SimplifiedExpression;
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
        ExpectedSimplifiedExpression[] expressions = java.util.Arrays.stream(expected)
                .map(VCTestUtils::parseExpectedSimplifiedExpression).toArray(ExpectedSimplifiedExpression[]::new);
        assertSimplifiedVC(implication, expressions);
    }

    public static void assertSimplifiedVC(VCImplication implication, ExpectedSimplifiedExpression... expected) {
        VCImplication current = implication;
        for (int i = 0; i < expected.length; i++) {
            ExpectedSimplifiedExpression expectedExpression = expected[i];
            SimplifiedExpression expression = simplifiedExpression(current, i);
            assertEquals(expectedExpression.simplified(), expression.getSimplifiedExpression().toString(),
                    "Unexpected simplified expression at implication " + i);
            if (expectedExpression.origin() != null)
                assertEquals(expectedExpression.origin(), expression.getOrigin().toString(),
                        "Unexpected origin expression at implication " + i);
            if (expectedExpression.binders() != null)
                assertEquals(expectedExpression.binders(), formatBinders(expression),
                        "Unexpected binders at implication " + i);
            current = current.getNext();
        }
        assertNull(current, "Expected VC chain to end after " + expected.length + " implications");
    }

    public static ExpectedSimplifiedExpression simplified(String simplified) {
        return new ExpectedSimplifiedExpression(simplified, null, null);
    }

    public static ExpectedSimplifiedExpression simplified(String simplified, String origin) {
        return new ExpectedSimplifiedExpression(simplified, origin, null);
    }

    public static ExpectedSimplifiedExpression simplified(String simplified, String origin, String binders) {
        return new ExpectedSimplifiedExpression(simplified, origin, binders);
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

    public static SimplifiedExpression simplifiedExpression(VCImplication implication, int index) {
        assertInstanceOf(SimplifiedExpression.class, implication.getRefinement().getExpression(),
                "Expected implication " + index + " to contain a SimplifiedExpression");
        return (SimplifiedExpression) implication.getRefinement().getExpression();
    }

    private static String formatBinders(SimplifiedExpression expression) {
        return expression.getBinders().stream().map(binder -> binder.getName() + ":" + binder.getType())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static ExpectedSimplifiedExpression parseExpectedSimplifiedExpression(String expected) {
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
        return new ExpectedSimplifiedExpression(simplified, origin, binders);
    }

    public record ExpectedSimplifiedExpression(String simplified, String origin, String binders) {
    }
}
