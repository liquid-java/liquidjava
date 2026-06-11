package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.assertSimplifiedVC;
import static liquidjava.utils.VCTestUtils.assertSimplificationSteps;
import static liquidjava.utils.VCTestUtils.assertVC;
import static liquidjava.utils.VCTestUtils.parse;
import static liquidjava.utils.VCTestUtils.simplified;
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
        assertSimplificationSteps(VCFolding::apply, vc("1 + 2 == 3"), simplified("3 == 3", "1 + 2 == 3"),
                simplified("true", "1 + 2 == 3"));
        assertFolded("4 > 7", "false");
    }

    @Test
    void foldsRealAndMixedNumericExpressions() {
        assertSimplificationSteps(VCFolding::apply, vc("1.5 + 2.0 == 3.5"),
                simplified("3.5 == 3.5", "1.5 + 2.0 == 3.5"), simplified("true", "1.5 + 2.0 == 3.5"));
        assertSimplificationSteps(VCFolding::apply, vc("2 + 0.5 > 2"), simplified("2.5 > 2", "2 + 0.5 > 2"),
                simplified("true", "2 + 0.5 > 2"));
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
        assertSimplificationSteps(VCFolding::apply, vc("-3 < 0"), simplified("-3 < 0", "-3 < 0"),
                simplified("true", "-3 < 0"));
    }

    @Test
    void foldsIteExpressions() {
        assertFolded("true ? a : b", "a");
        assertFolded("false ? a : b", "b");
        assertFolded("cond ? b : b", "b");
    }

    @Test
    void foldsIteBranchesBeforeComparingThem() {
        assertSimplificationSteps(VCFolding::apply, vc("cond ? 1 + 2 : 3"),
                simplified("cond ? 3 : 3", "cond ? 1 + 2 : 3"), simplified("3", "cond ? 1 + 2 : 3"));
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

        assertSimplificationSteps(VCFolding::apply, implication, simplified("3 == 3", "Config.LIMIT == 3"),
                simplified("true", "Config.LIMIT == 3"));
    }

    @Test
    void foldsResolvedEnumLiteralsInsideLargerExpression() {
        Enum limit = new Enum("Config", "LIMIT");
        limit.setResolvedLiteral(new LiteralInt(3));
        BinaryExpression arithmetic = new BinaryExpression(limit, "+", new LiteralInt(2));
        VCImplication implication = new VCImplication(
                new Predicate(new BinaryExpression(arithmetic, "==", new LiteralInt(5))));

        assertSimplificationSteps(VCFolding::apply, implication, simplified("3 + 2 == 5", "Config.LIMIT + 2 == 5"),
                simplified("5 == 5", "Config.LIMIT + 2 == 5"), simplified("true", "Config.LIMIT + 2 == 5"));
    }

    @Test
    void preservesOriginFromExistingSimplifiedImplication() {
        VCImplication substituted = VCSubstitution.apply(vc("∀x:int. x == 1", "x + 1 + 2 > 0"));

        assertSimplificationSteps(VCFolding::apply, substituted, simplified("2 + 2 > 0", "∀x:int. x + 1 + 2 > 0"),
                simplified("4 > 0", "∀x:int. x + 1 + 2 > 0"), simplified("true", "∀x:int. x + 1 + 2 > 0"));
    }

    @Test
    void recordsOriginWhenOnlyGroupIsUnwrapped() {
        VCImplication implication = new VCImplication(new Predicate(new GroupExpression(parse("x > 0"))));

        VCImplication result = VCFolding.apply(implication);

        SimplifiedVCImplication simplified = assertInstanceOf(SimplifiedVCImplication.class, result);
        assertEquals("x > 0", simplified.getRefinement().toString());
        assertInstanceOf(GroupExpression.class, simplified.getOrigin().getRefinement().getExpression());
    }

    @Test
    void recordsOriginWhenFoldingLaterImplication() {
        VCImplication implication = vc("x > 0", "1 + 2 > 0");

        VCImplication result = VCFolding.apply(implication);

        assertEquals("x > 0", result.getRefinement().toString());
        SimplifiedVCImplication simplifiedNext = assertInstanceOf(SimplifiedVCImplication.class, result.getNext());
        assertEquals("3 > 0", simplifiedNext.getRefinement().toString());
        assertEquals("1 + 2 > 0", simplifiedNext.getOrigin().getRefinement().toString());

        result = VCFolding.apply(result);

        assertEquals("x > 0", result.getRefinement().toString());
        simplifiedNext = assertInstanceOf(SimplifiedVCImplication.class, result.getNext());
        assertEquals("true", simplifiedNext.getRefinement().toString());
        assertEquals("1 + 2 > 0", simplifiedNext.getOrigin().getRefinement().toString());
    }

    private static void assertFolded(String original, String folded) {
        VCImplication result = VCFolding.apply(vc(original));

        assertSimplifiedVC(result, simplified(folded, original));
    }

    private static void assertUnchanged(String original) {
        VCImplication result = VCFolding.apply(vc(original));

        assertVC(result, original);
    }
}
