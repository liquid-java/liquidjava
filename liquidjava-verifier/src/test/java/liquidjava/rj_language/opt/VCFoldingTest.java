package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.LiteralInt;
import org.junit.jupiter.api.Test;

class VCFoldingTest {

    private final VCFolding folding = new VCFolding();

    @Test
    void foldsIntegerArithmeticAndComparisons() {
        VCImplication implication = vc("1 + 2 == 3");

        assertSimplificationSteps(folding, implication, step("3 == 3"), step("true"));
        assertSimplificationSteps(folding, vc("4 > 7"), step("false"));
    }

    @Test
    void foldsRealAndMixedNumericExpressions() {
        VCImplication realArithmetic = vc("1.5 + 2.0 == 3.5");
        VCImplication mixedArithmetic = vc("2 + 0.5 > 2");

        assertSimplificationSteps(folding, realArithmetic, step("3.5 == 3.5"), step("true"));
        assertSimplificationSteps(folding, mixedArithmetic, step("2.5 > 2"), step("true"));
    }

    @Test
    void leavesDivisionAndModuloByZeroUnchanged() {
        assertSimplificationSteps(folding, vc("4 / 0 == 0"), step("4 / 0 == 0"));
        assertSimplificationSteps(folding, vc("4 % 0 == 0"), step("4 % 0 == 0"));
    }

    @Test
    void leavesRealDivisionAndModuloByZeroUnchanged() {
        assertSimplificationSteps(folding, vc("4.0 / 0.0 == 0.0"), step("4.0 / 0.0 == 0.0"));
        assertSimplificationSteps(folding, vc("4.0 % 0.0 == 0.0"), step("4.0 % 0.0 == 0.0"));
    }

    @Test
    void foldsIntegerDivisionTowardZeroForNegativeResults() {
        VCImplication implication = vc("(2 - 7) / 2 == -2");

        assertSimplificationSteps(folding, implication, step("(2 - 7) / 2 == -2"), step("-5 / 2 == -2"),
                step("-2 == -2"), step("-2 == -2"), step("true"));
    }

    @Test
    void foldsIntegerModuloWithJavaSignedRemainder() {
        VCImplication negativeDividend = vc("-5 % 2 < 0");
        VCImplication negativeDivisor = vc("5 % -2 > 0");

        assertSimplificationSteps(folding, negativeDividend, step("-5 % 2 < 0"), step("-1 < 0"), step("true"));
        assertSimplificationSteps(folding, negativeDivisor, step("5 % -2 > 0"), step("1 > 0"), step("true"));
    }

    @Test
    void foldsBooleanBinaryExpressions() {
        assertSimplificationSteps(folding, vc("true && false"), step("false"));
        assertSimplificationSteps(folding, vc("false --> true"), step("true"));
        assertSimplificationSteps(folding, vc("true != false"), step("true"));
    }

    @Test
    void foldsBooleanSubexpressionsInsideLargerExpression() {
        assertSimplificationSteps(folding, vc("true && false || ok"), step("false || ok"));
    }

    @Test
    void foldsNestedConstantsInsideLargerExpression() {
        assertSimplificationSteps(folding, vc("x > 1 + 2"), step("x > 3"));
        assertSimplificationSteps(folding, vc("x + 1 + 2 > 4"), step("x + 3 > 4"));
    }

    @Test
    void foldsPartialComparisonsWithoutDroppingSymbolicTerms() {
        assertSimplificationSteps(folding, vc("1 + 2 < x + 4"), step("3 < x + 4"));
    }

    @Test
    void foldsUnaryExpressions() {
        assertSimplificationSteps(folding, vc("!true"), step("false"));
        VCImplication implication = vc("-3 < 0");

        assertSimplificationSteps(folding, implication, step("-3 < 0"), step("true"));
    }

    @Test
    void foldsIteExpressions() {
        assertSimplificationSteps(folding, vc("true ? a : b"), step("a"));
        assertSimplificationSteps(folding, vc("false ? a : b"), step("b"));
        assertSimplificationSteps(folding, vc("cond ? b : b"), step("b"));
    }

    @Test
    void foldsIteBranchesBeforeComparingThem() {
        VCImplication implication = vc("cond ? 1 + 2 : 3");

        assertSimplificationSteps(folding, implication, step("cond ? 3 : 3"), step("3"));
    }

    @Test
    void foldsAdjacentIntegerConstants() {
        assertSimplificationSteps(folding, vc("x + 1 - 2"), step("x - 1"));
        assertSimplificationSteps(folding, vc("x - 1 + 2"), step("x + 1"));
        assertSimplificationSteps(folding, vc("x + 1 + 2"), step("x + 3"));
        assertSimplificationSteps(folding, vc("x + 1 - 1"), step("x"));
    }

    @Test
    void foldsEnumEqualityAndInequality() {
        assertSimplificationSteps(folding, vc("Mode.Photo == Mode.Photo"), step("true"));
        assertSimplificationSteps(folding, vc("Mode.Photo != Mode.Video"), step("true"));
    }

    @Test
    void foldsResolvedEnumLiterals() {
        Enum limit = new Enum("Config", "LIMIT");
        limit.setResolvedLiteral(new LiteralInt(3));
        VCImplication implication = new VCImplication(
                new Predicate(new BinaryExpression(limit, "==", new LiteralInt(3))));

        assertSimplificationSteps(folding, implication, step("3 == 3"), step("true"));
    }

    @Test
    void foldsResolvedEnumLiteralsInsideLargerExpression() {
        Enum limit = new Enum("Config", "LIMIT");
        limit.setResolvedLiteral(new LiteralInt(3));
        BinaryExpression arithmetic = new BinaryExpression(limit, "+", new LiteralInt(2));
        VCImplication implication = new VCImplication(
                new Predicate(new BinaryExpression(arithmetic, "==", new LiteralInt(5))));

        assertSimplificationSteps(folding, implication, step("3 + 2 == 5"), step("5 == 5"), step("true"));
    }

    @Test
    void recordsOriginWhenOnlyGroupIsUnwrapped() {
        VCImplication implication = vc("(x > 0)");
        VCSimplificationResult result = assertSimplificationSteps(folding, implication, step("x > 0"));

        assertEquals("x > 0", result.getImplication().getRefinement().toString());
    }

    @Test
    void recordsOriginWhenFoldingLaterImplication() {
        VCImplication implication = vc("x > 0", "1 + 2 > 0");

        VCSimplificationResult result = assertSimplificationSteps(folding, implication, step("x > 0", "3 > 0"));

        result = assertSimplificationSteps(folding, result.getImplication(), step("x > 0", "true"));
        assertEquals("true", result.getImplication().getNext().getRefinement().getExpression().toDisplayString());
    }

}
