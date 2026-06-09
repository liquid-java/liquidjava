package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.assertSimplifiedVC;
import static liquidjava.utils.VCTestUtils.assertVC;
import static liquidjava.utils.VCTestUtils.simplified;
import static liquidjava.utils.VCTestUtils.vc;
import static org.junit.jupiter.api.Assertions.assertNull;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.LiteralInt;
import org.junit.jupiter.api.Test;

class VCFoldingTest {

    @Test
    void applyReturnsNullForNullImplication() {
        assertNull(VCFolding.apply(null));
    }

    @Test
    void foldsIntegerArithmeticAndComparisons() {
        assertFolded("1 + 2 == 3", "true");
        assertFolded("4 > 7", "false");
    }

    @Test
    void foldsRealAndMixedNumericExpressions() {
        assertFolded("1.5 + 2.0 == 3.5", "true");
        assertFolded("2 + 0.5 > 2", "true");
    }

    @Test
    void leavesDivisionAndModuloByZeroUnchanged() {
        assertUnchanged("4 / 0 == 0");
        assertUnchanged("4 % 0 == 0");
    }

    @Test
    void foldsBooleanBinaryExpressions() {
        assertFolded("true && false", "false");
        assertFolded("false --> true", "true");
        assertFolded("true != false", "true");
    }

    @Test
    void foldsUnaryExpressions() {
        assertFolded("!true", "false");
        assertFolded("-3 < 0", "true");
    }

    @Test
    void foldsIteExpressions() {
        assertFolded("true ? a : b", "a");
        assertFolded("false ? a : b", "b");
        assertFolded("cond ? b : b", "b");
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

        VCImplication result = VCFolding.apply(implication);

        assertSimplifiedVC(result, simplified("true", "Config.LIMIT == 3"));
    }

    @Test
    void preservesOriginFromExistingSimplifiedImplication() {
        VCImplication substituted = VCSubstitution.apply(vc("∀x:int. x == 1", "x + 1 + 2 > 0"));

        VCImplication result = VCFolding.apply(substituted);

        assertSimplifiedVC(result, simplified("true", "∀x:int. x + 1 + 2 > 0"));
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
