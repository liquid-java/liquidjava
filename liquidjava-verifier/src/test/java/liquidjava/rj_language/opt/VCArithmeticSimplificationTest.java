package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import org.junit.jupiter.api.Test;

class VCArithmeticSimplificationTest {

    @Test
    void applyReturnsNullForNullImplication() {
        assertNull(VCArithmeticSimplification.apply(null));
    }

    @Test
    void simplifiesAdditiveIdentities() {
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x + 0 > 0"),
                chain(expect("x > 0", "x + 0 > 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("0 + x > 0"),
                chain(expect("x > 0", "0 + x > 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x - 0 > 0"),
                chain(expect("x > 0", "x - 0 > 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("0 - x > 0"),
                chain(expect("-x > 0", "0 - x > 0")));
    }

    @Test
    void simplifiesNegatedAdditionAndSubtraction() {
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x + -x == 0"),
                chain(expect("0 == 0", "x + -x == 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("-x + x == 0"),
                chain(expect("0 == 0", "-x + x == 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x - x == 0"),
                chain(expect("0 == 0", "x - x == 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("--x == x"),
                chain(expect("x == x", "-(-x) == x")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x + -y == 0"),
                chain(expect("x - y == 0", "x + -y == 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x - -y == 0"),
                chain(expect("x + y == 0", "x - -y == 0")));
    }

    @Test
    void simplifiesMultiplicativeIdentities() {
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x * 1 > 0"),
                chain(expect("x > 0", "x * 1 > 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("1 * x > 0"),
                chain(expect("x > 0", "1 * x > 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x * 0 == 0"),
                chain(expect("0 == 0", "x * 0 == 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("0 * x == 0"),
                chain(expect("0 == 0", "0 * x == 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x / 1 > 0"),
                chain(expect("x > 0", "x / 1 > 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x % 1 == 0"),
                chain(expect("0 == 0", "x % 1 == 0")));
    }

    @Test
    void simplifiesGuardedDivisionAndModuloIdentities() {
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x != 0", "0 / x == 0"),
                chain(expect("x != 0", "x != 0"), expect("0 == 0", "0 / x == 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x != 0", "x / x == 1"),
                chain(expect("x != 0", "x != 0"), expect("1 == 1", "x / x == 1")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("0 != x", "x % x == 0"),
                chain(expect("0 != x", "0 != x"), expect("0 == 0", "x % x == 0")));
    }

    @Test
    void simplifiesGuardedDivisionAndModuloIdentitiesWhenEqualityImpliesNonZero() {
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x == 1", "0 / x == 0"),
                chain(expect("x == 1", "x == 1"), expect("0 == 0", "0 / x == 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("1 == x", "x / x == 1"),
                chain(expect("1 == x", "1 == x"), expect("1 == 1", "x / x == 1")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x == 1", "x % x == 0"),
                chain(expect("x == 1", "x == 1"), expect("0 == 0", "x % x == 0")));
    }

    @Test
    void leavesUnguardedDivisionAndModuloIdentitiesUnchanged() {
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("0 / x == 0"),
                chain(expect("0 / x == 0", "0 / x == 0")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x / x == 1"),
                chain(expect("x / x == 1", "x / x == 1")));
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x % x == 0"),
                chain(expect("x % x == 0", "x % x == 0")));
    }

    @Test
    void simplifiesOnlyFirstArithmeticIdentity() {
        assertSimplificationSteps(VCArithmeticSimplification::apply, vc("x + 0 + 1 > 0"),
                chain(expect("x + 1 > 0", "x + 0 + 1 > 0")));
    }

    @Test
    void recordsOriginWhenSimplifyingLaterImplication() {
        VCImplication implication = vc("x > 0", "y + 0 > x");

        VCImplication result = assertSimplificationSteps(VCArithmeticSimplification::apply, implication,
                chain(expect("x > 0", "x > 0"), expect("y > x", "y + 0 > x")));

        SimplifiedVCImplication simplifiedNext = assertInstanceOf(SimplifiedVCImplication.class, result.getNext());
        assertEquals("y + 0 > x", simplifiedNext.getOrigin().getRefinement().getExpression().toDisplayString());
    }

    @Test
    void recordsCurrentImplicationAsOriginWhenSimplifyingExistingSimplifiedImplication() {
        VCImplication substituted = VCSubstitution.apply(vc("∀x:int. x == y + 0", "x > 0"));

        assertSimplificationSteps(VCArithmeticSimplification::apply, substituted, chain(expect("y > 0", "y + 0 > 0")));
    }
}
