package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.assertSimplificationSteps;
import static liquidjava.utils.VCTestUtils.vc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.LiteralInt;
import org.junit.jupiter.api.Test;

class VCFoldingTest {

    @Test
    void applyReturnsNullForNullImplication() {
        assertNull(VCFolding.apply(null));
    }

    @Test
    void foldsIntegerArithmeticAndComparisons() {
        VCImplication implication = vc("1 + 2 == 3");

        assertSimplificationSteps(VCFolding::apply, implication, "3 == 3", "true");
        assertFolded("4 > 7", "false");
    }

    @Test
    void foldsRealAndMixedNumericExpressions() {
        VCImplication realArithmetic = vc("1.5 + 2.0 == 3.5");
        VCImplication mixedArithmetic = vc("2 + 0.5 > 2");

        assertSimplificationSteps(VCFolding::apply, realArithmetic, "3.5 == 3.5", "true");
        assertSimplificationSteps(VCFolding::apply, mixedArithmetic, "2.5 > 2", "true");
    }

    @Test
    void leavesDivisionAndModuloByZeroUnchanged() {
        assertUnchanged("4 / 0 == 0");
        assertUnchanged("4 % 0 == 0");
    }

    @Test
    void leavesRealDivisionAndModuloByZeroUnchanged() {
        assertUnchanged("4.0 / 0.0 == 0.0");
        assertUnchanged("4.0 % 0.0 == 0.0");
    }

    @Test
    void foldsBooleanBinaryExpressions() {
        assertFolded("true && false", "false");
        assertFolded("false --> true", "true");
        assertFolded("true != false", "true");
    }

    @Test
    void foldsBooleanSubexpressionsInsideLargerExpression() {
        assertFolded("true && false || ok", "false || ok");
    }

    @Test
    void foldsNestedConstantsInsideLargerExpression() {
        assertFolded("x > 1 + 2", "x > 3");
        assertFolded("x + 1 + 2 > 4", "x + 3 > 4");
    }

    @Test
    void foldsPartialComparisonsWithoutDroppingSymbolicTerms() {
        assertFolded("1 + 2 < x + 4", "3 < x + 4");
    }

    @Test
    void foldsUnaryExpressions() {
        assertFolded("!true", "false");
        VCImplication implication = vc("-3 < 0");

        assertSimplificationSteps(VCFolding::apply, implication, "-3 < 0", "true");
    }

    @Test
    void foldsIteExpressions() {
        assertFolded("true ? a : b", "a");
        assertFolded("false ? a : b", "b");
        assertFolded("cond ? b : b", "b");
    }

    @Test
    void foldsIteBranchesBeforeComparingThem() {
        VCImplication implication = vc("cond ? 1 + 2 : 3");

        assertSimplificationSteps(VCFolding::apply, implication, "cond ? 3 : 3", "3");
    }

    @Test
    void foldsAdjacentIntegerConstants() {
        assertFolded("x + 1 - 2", "x - 1");
        assertFolded("x - 1 + 2", "x + 1");
        assertFolded("x + 1 + 2", "x + 3");
        assertFolded("x + 1 - 1", "x");
    }

    @Test
    void foldsEnumEqualityAndInequality() {
        assertFolded("Mode.Photo == Mode.Photo", "true");
        assertFolded("Mode.Photo != Mode.Video", "true");
    }

    @Test
    void foldsResolvedEnumLiterals() {
        Enum limit = new Enum("Config", "LIMIT");
        limit.setResolvedLiteral(new LiteralInt(3));
        VCImplication implication = new VCImplication(
                new Predicate(new BinaryExpression(limit, "==", new LiteralInt(3))));

        assertSimplificationSteps(VCFolding::apply, implication, "3 == 3", "true");
    }

    @Test
    void foldsResolvedEnumLiteralsInsideLargerExpression() {
        Enum limit = new Enum("Config", "LIMIT");
        limit.setResolvedLiteral(new LiteralInt(3));
        BinaryExpression arithmetic = new BinaryExpression(limit, "+", new LiteralInt(2));
        VCImplication implication = new VCImplication(
                new Predicate(new BinaryExpression(arithmetic, "==", new LiteralInt(5))));

        assertSimplificationSteps(VCFolding::apply, implication, "3 + 2 == 5", "5 == 5", "true");
    }

    @Test
    void preservesOriginFromExistingSimplifiedImplication() {
        VCImplication substituted = VCSubstitution.apply(vc("∀x:int. x == 1", "x + 1 + 2 > 0"));

        assertSimplificationSteps(VCFolding::apply, substituted, "2 + 2 > 0", "4 > 0", "true");
    }

    @Test
    void recordsOriginWhenOnlyGroupIsUnwrapped() {
        VCImplication implication = vc("(x > 0)");
        VCImplication result = assertSimplificationSteps(VCFolding::apply, implication, "x > 0");

        SimplifiedVCImplication simplified = assertInstanceOf(SimplifiedVCImplication.class, result);
        assertEquals("x > 0", simplified.getRefinement().toString());
        assertInstanceOf(GroupExpression.class, simplified.getOrigin().getRefinement().getExpression());
    }

    @Test
    void recordsOriginWhenFoldingLaterImplication() {
        VCImplication implication = vc("x > 0", "1 + 2 > 0");

        VCImplication result = assertSimplificationSteps(VCFolding::apply, implication, "x > 0 -> 3 > 0");

        SimplifiedVCImplication simplifiedNext = assertInstanceOf(SimplifiedVCImplication.class, result.getNext());
        assertEquals("1 + 2 > 0", simplifiedNext.getOrigin().getRefinement().toString());

        result = assertSimplificationSteps(VCFolding::apply, result, "x > 0 -> true");

        simplifiedNext = assertInstanceOf(SimplifiedVCImplication.class, result.getNext());
        assertEquals("1 + 2 > 0", simplifiedNext.getOrigin().getRefinement().toString());
    }

    private static void assertFolded(String original, String folded) {
        VCImplication implication = vc(original);

        assertSimplificationSteps(VCFolding::apply, implication, folded);
    }

    private static void assertUnchanged(String original) {
        VCImplication implication = vc(original);

        assertSimplificationSteps(VCFolding::apply, implication, original);
    }
}
