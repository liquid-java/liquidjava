package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
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
        assertSimplificationSteps(folding, vc("1 + 2 == 3"), step("3 == 3"), step("true"));
        assertSimplificationSteps(folding, vc("4 > 7"), step("false"));
    }

    @Test
    void foldsRealAndMixedNumericExpressions() {
        assertSimplificationSteps(folding, vc("1.5 + 2.0 == 3.5"), step("3.5 == 3.5"), step("true"));
        assertSimplificationSteps(folding, vc("2 + 0.5 > 2"), step("2.5 > 2"), step("true"));
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
        assertSimplificationSteps(folding, vc("(2 - 7) / 2 == -2"), step("-5 / 2 == -2"), step("-2 == -2"),
                step("-2 == -2"), step("true"));
    }

    @Test
    void foldsIntegerModuloWithJavaSignedRemainder() {
        assertSimplificationSteps(folding, vc("-5 % 2 < 0"), step("-5 % 2 < 0"), step("-1 < 0"), step("true"));
        assertSimplificationSteps(folding, vc("5 % -2 > 0"), step("5 % -2 > 0"), step("1 > 0"), step("true"));
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
        assertSimplificationSteps(folding, vc("-3 < 0"), step("-3 < 0"), step("true"));
    }

    @Test
    void foldsIteExpressions() {
        assertSimplificationSteps(folding, vc("true ? a : b"), step("a"));
        assertSimplificationSteps(folding, vc("false ? a : b"), step("b"));
        assertSimplificationSteps(folding, vc("cond ? b : b"), step("b"));
    }

    @Test
    void foldsIteBranchesBeforeComparingThem() {
        assertSimplificationSteps(folding, vc("cond ? 1 + 2 : 3"), step("cond ? 3 : 3"), step("3"));
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
    void leavesParenthesizedExpressionUnchanged() {
        assertSimplificationSteps(folding, vc("(x > 0)"), step("x > 0"));
    }

    @Test
    void recordsOriginWhenFoldingLaterImplication() {
        assertSimplificationSteps(folding, vc("x > 0", "1 + 2 > 0"), step("x > 0", "3 > 0"), step("x > 0", "true"));
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
}
