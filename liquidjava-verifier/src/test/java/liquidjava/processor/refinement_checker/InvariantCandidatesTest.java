package liquidjava.processor.refinement_checker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import spoon.Launcher;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtWhile;
import spoon.reflect.visitor.filter.TypeFilter;

class InvariantCandidatesTest {

    @Test
    void growsOrShrinksFromItsValueBeforeTheLoop() {
        assertEquals(List.of("i >= i0", "i <= i0"), candidates("i", "i0", "true"));
    }

    @Test
    void noBoundsFromBeforeWithoutAnInstance() {
        assertEquals(List.of("i <= n"), candidates("i", null, "i < n"));
    }

    @Test
    void weakensTheComparisonsOfTheCondition() {
        assertEquals(List.of("i >= i0", "i <= i0", "i <= n", "0 <= i", "i >= n - 1", "2 * i >= -n"),
                candidates("i", "i0", "i < n && 0 <= i && i > n - 1 && 2 * i >= -n"));
    }

    @Test
    void keepsTheGroupingOfTerms() {
        // printed without parentheses: the right-hand side is n - (n - 1), not n - n - 1
        Predicate candidate = InvariantCandidates.of("i", Optional.empty(), condition("i < n - (n - 1)")).get(0);
        BinaryExpression bound = (BinaryExpression) candidate.getExpression().getChildren().get(1);
        assertEquals("n - 1", bound.getSecondOperand().toString());
    }

    @Test
    void onlyComparisonsThatMentionTheVariable() {
        assertEquals(List.of("i <= 10"), candidates("i", null, "n > 0 && i < 10"));
    }

    @Test
    void onlyComparisonsTheConditionImplies() {
        assertEquals(List.of(), candidates("i", null, "i < n || i > 10"));
        assertEquals(List.of(), candidates("i", null, "!(i < n)"));
        assertEquals(List.of(), candidates("i", null, "i != n"));
    }

    @Test
    void onlyTermsOverLiteralsAndIntLocals() {
        assertEquals(List.of(), candidates("i", null, "i < field && i < size() && i < (int) d && i < n / 2"));
        assertEquals(List.of(), candidates("i", null, "i < 10L"));
    }

    @Test
    void atMostAFixedNumberPerVariable() {
        assertEquals(InvariantCandidates.MAX_PER_VARIABLE,
                InvariantCandidates
                        .of("i", Optional.of("i0"),
                                condition("i < n && i < n + 1 && i < n + 2 && i < n + 3 && i < n + 4 && i < n + 5"))
                        .size());
    }

    private static List<String> candidates(String variable, String before, String condition) {
        return InvariantCandidates.of(variable, Optional.ofNullable(before), condition(condition)).stream()
                .map(Predicate::toString).toList();
    }

    /** The condition of a loop in a method with int locals i and n, an int field, a double d and a size() method */
    @SuppressWarnings("unchecked")
    private static CtExpression<Boolean> condition(String condition) {
        var type = Launcher
                .parseClass("class C { int field; int size() { return 0; } void f(int n, double d) { int i0 = 0;"
                        + " int i = 0; while (" + condition + ") { i++; } } }");
        return type.getElements(new TypeFilter<>(CtWhile.class)).get(0).getLoopingExpression();
    }
}
