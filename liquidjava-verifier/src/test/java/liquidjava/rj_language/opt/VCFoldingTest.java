package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
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

        assertSimplificationSteps(VCFolding::apply, implication, chain(expect("3 == 3", "1 + 2 == 3")),
                chain(expect("true", "1 + 2 == 3")));
        assertSimplificationSteps(VCFolding::apply, vc("4 > 7"), chain(expect("false", "4 > 7")));
    }

    @Test
    void foldsRealAndMixedNumericExpressions() {
        VCImplication realArithmetic = vc("1.5 + 2.0 == 3.5");
        VCImplication mixedArithmetic = vc("2 + 0.5 > 2");

        assertSimplificationSteps(VCFolding::apply, realArithmetic, chain(expect("3.5 == 3.5", "1.5 + 2.0 == 3.5")),
                chain(expect("true", "1.5 + 2.0 == 3.5")));
        assertSimplificationSteps(VCFolding::apply, mixedArithmetic, chain(expect("2.5 > 2", "2 + 0.5 > 2")),
                chain(expect("true", "2 + 0.5 > 2")));
    }

    @Test
    void leavesDivisionAndModuloByZeroUnchanged() {
        assertSimplificationSteps(VCFolding::apply, vc("4 / 0 == 0"), chain(expect("4 / 0 == 0", "4 / 0 == 0")));
        assertSimplificationSteps(VCFolding::apply, vc("4 % 0 == 0"), chain(expect("4 % 0 == 0", "4 % 0 == 0")));
    }

    @Test
    void leavesRealDivisionAndModuloByZeroUnchanged() {
        assertSimplificationSteps(VCFolding::apply, vc("4.0 / 0.0 == 0.0"),
                chain(expect("4.0 / 0.0 == 0.0", "4.0 / 0.0 == 0.0")));
        assertSimplificationSteps(VCFolding::apply, vc("4.0 % 0.0 == 0.0"),
                chain(expect("4.0 % 0.0 == 0.0", "4.0 % 0.0 == 0.0")));
    }

    @Test
    void foldsBooleanBinaryExpressions() {
        assertSimplificationSteps(VCFolding::apply, vc("true && false"), chain(expect("false", "true && false")));
        assertSimplificationSteps(VCFolding::apply, vc("false --> true"), chain(expect("true", "false --> true")));
        assertSimplificationSteps(VCFolding::apply, vc("true != false"), chain(expect("true", "true != false")));
    }

    @Test
    void foldsBooleanSubexpressionsInsideLargerExpression() {
        assertSimplificationSteps(VCFolding::apply, vc("true && false || ok"),
                chain(expect("false || ok", "true && false || ok")));
    }

    @Test
    void foldsNestedConstantsInsideLargerExpression() {
        assertSimplificationSteps(VCFolding::apply, vc("x > 1 + 2"), chain(expect("x > 3", "x > 1 + 2")));
        assertSimplificationSteps(VCFolding::apply, vc("x + 1 + 2 > 4"), chain(expect("x + 3 > 4", "x + 1 + 2 > 4")));
    }

    @Test
    void foldsPartialComparisonsWithoutDroppingSymbolicTerms() {
        assertSimplificationSteps(VCFolding::apply, vc("1 + 2 < x + 4"), chain(expect("3 < x + 4", "1 + 2 < x + 4")));
    }

    @Test
    void foldsUnaryExpressions() {
        assertSimplificationSteps(VCFolding::apply, vc("!true"), chain(expect("false", "!true")));
        VCImplication implication = vc("-3 < 0");

        assertSimplificationSteps(VCFolding::apply, implication, chain(expect("-3 < 0", "-3 < 0")),
                chain(expect("true", "-3 < 0")));
    }

    @Test
    void foldsIteExpressions() {
        assertSimplificationSteps(VCFolding::apply, vc("true ? a : b"), chain(expect("a", "true ? a : b")));
        assertSimplificationSteps(VCFolding::apply, vc("false ? a : b"), chain(expect("b", "false ? a : b")));
        assertSimplificationSteps(VCFolding::apply, vc("cond ? b : b"), chain(expect("b", "cond ? b : b")));
    }

    @Test
    void foldsIteBranchesBeforeComparingThem() {
        VCImplication implication = vc("cond ? 1 + 2 : 3");

        assertSimplificationSteps(VCFolding::apply, implication, chain(expect("cond ? 3 : 3", "cond ? 1 + 2 : 3")),
                chain(expect("3", "cond ? 1 + 2 : 3")));
    }

    @Test
    void foldsAdjacentIntegerConstants() {
        assertSimplificationSteps(VCFolding::apply, vc("x + 1 - 2"), chain(expect("x - 1", "x + 1 - 2")));
        assertSimplificationSteps(VCFolding::apply, vc("x - 1 + 2"), chain(expect("x + 1", "x - 1 + 2")));
        assertSimplificationSteps(VCFolding::apply, vc("x + 1 + 2"), chain(expect("x + 3", "x + 1 + 2")));
        assertSimplificationSteps(VCFolding::apply, vc("x + 1 - 1"), chain(expect("x", "x + 1 - 1")));
    }

    @Test
    void foldsEnumEqualityAndInequality() {
        assertSimplificationSteps(VCFolding::apply, vc("Mode.Photo == Mode.Photo"),
                chain(expect("true", "Mode.Photo == Mode.Photo")));
        assertSimplificationSteps(VCFolding::apply, vc("Mode.Photo != Mode.Video"),
                chain(expect("true", "Mode.Photo != Mode.Video")));
    }

    @Test
    void foldsResolvedEnumLiterals() {
        Enum limit = new Enum("Config", "LIMIT");
        limit.setResolvedLiteral(new LiteralInt(3));
        VCImplication implication = new VCImplication(
                new Predicate(new BinaryExpression(limit, "==", new LiteralInt(3))));

        assertSimplificationSteps(VCFolding::apply, implication, chain(expect("3 == 3", "Config.LIMIT == 3")),
                chain(expect("true", "Config.LIMIT == 3")));
    }

    @Test
    void foldsResolvedEnumLiteralsInsideLargerExpression() {
        Enum limit = new Enum("Config", "LIMIT");
        limit.setResolvedLiteral(new LiteralInt(3));
        BinaryExpression arithmetic = new BinaryExpression(limit, "+", new LiteralInt(2));
        VCImplication implication = new VCImplication(
                new Predicate(new BinaryExpression(arithmetic, "==", new LiteralInt(5))));

        assertSimplificationSteps(VCFolding::apply, implication, chain(expect("3 + 2 == 5", "Config.LIMIT + 2 == 5")),
                chain(expect("5 == 5", "Config.LIMIT + 2 == 5")), chain(expect("true", "Config.LIMIT + 2 == 5")));
    }

    @Test
    void preservesOriginFromExistingSimplifiedImplication() {
        VCImplication substituted = VCSubstitution.apply(vc("∀x:int. x == 1", "x + 1 + 2 > 0"));

        assertSimplificationSteps(VCFolding::apply, substituted, chain(expect("2 + 2 > 0", "∀x:int. x + 1 + 2 > 0")),
                chain(expect("4 > 0", "∀x:int. x + 1 + 2 > 0")), chain(expect("true", "∀x:int. x + 1 + 2 > 0")));
    }

    @Test
    void recordsOriginWhenOnlyGroupIsUnwrapped() {
        VCImplication implication = vc("(x > 0)");
        VCImplication result = assertSimplificationSteps(VCFolding::apply, implication,
                chain(expect("x > 0", "x > 0")));

        SimplifiedVCImplication simplified = assertInstanceOf(SimplifiedVCImplication.class, result);
        assertEquals("x > 0", simplified.getRefinement().toString());
        assertInstanceOf(GroupExpression.class, simplified.getOrigin().getRefinement().getExpression());
    }

    @Test
    void recordsOriginWhenFoldingLaterImplication() {
        VCImplication implication = vc("x > 0", "1 + 2 > 0");

        VCImplication result = assertSimplificationSteps(VCFolding::apply, implication,
                chain(expect("x > 0", "x > 0"), expect("3 > 0", "1 + 2 > 0")));

        SimplifiedVCImplication simplifiedNext = assertInstanceOf(SimplifiedVCImplication.class, result.getNext());
        assertEquals("1 + 2 > 0", simplifiedNext.getOrigin().getRefinement().toString());

        result = assertSimplificationSteps(VCFolding::apply, result,
                chain(expect("x > 0", "x > 0"), expect("true", "1 + 2 > 0")));

        simplifiedNext = assertInstanceOf(SimplifiedVCImplication.class, result.getNext());
        assertEquals("1 + 2 > 0", simplifiedNext.getOrigin().getRefinement().toString());
    }

}
