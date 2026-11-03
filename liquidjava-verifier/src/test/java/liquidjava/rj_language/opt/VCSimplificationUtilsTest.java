package liquidjava.rj_language.opt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.parsing.RefinementsParser;

class VCSimplificationUtilsTest {

    private static Expression parse(String refinement) {
        return RefinementsParser.createAST(refinement, "");
    }

    private static List<String> display(List<Expression> expressions) {
        return expressions.stream().map(Expression::toDisplayString).toList();
    }

    @Test
    void splitsConjunctionsAndNegatedDisjunctions() {
        assertEquals(List.of("!a", "!(b > 0)", "c", "d"),
                display(VCSimplificationUtils.splitConjuncts(parse("!(a || b > 0) && c && !!d"))));
    }

    @Test
    void keepsDisjunctionsAndNegatedConjunctionsWhole() {
        assertEquals(List.of("a || b", "!(c && d)"),
                display(VCSimplificationUtils.splitConjuncts(parse("(a || b) && !(c && d)"))));
    }

    @Test
    void linksConjunctsSharingVariablesWithTheSeedTransitively() {
        List<Expression> conjuncts = List.of(parse("y == x + 1"), parse("p"), parse("x > z"), parse("q == p"));
        assertEquals(List.of("y == x + 1", "x > z"), display(
                VCSimplificationUtils.linkedConjuncts(conjuncts, VCSimplificationUtils::variablesOf, List.of("y"))));
    }

    @Test
    void linksNothingWithoutASharedVariable() {
        assertEquals(List.of(), VCSimplificationUtils.linkedConjuncts(List.of(parse("p && q")),
                VCSimplificationUtils::variablesOf, List.of("t")));
    }
}
