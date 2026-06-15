package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import org.junit.jupiter.api.Test;

class VCArithmeticSimplificationTest {

    private final VCArithmeticSimplification simplification = new VCArithmeticSimplification();

    @Test
    void simplifiesAdditiveIdentities() {
        assertSimplificationSteps(simplification::apply, vc("x + 0 > 0"), chain(expect("x > 0", "x + 0 > 0")));
        assertSimplificationSteps(simplification::apply, vc("0 + x > 0"), chain(expect("x > 0", "0 + x > 0")));
        assertSimplificationSteps(simplification::apply, vc("x - 0 > 0"), chain(expect("x > 0", "x - 0 > 0")));
        assertSimplificationSteps(simplification::apply, vc("0 - x > 0"), chain(expect("-x > 0", "0 - x > 0")));
    }

    @Test
    void simplifiesNegatedAdditionAndSubtraction() {
        assertSimplificationSteps(simplification::apply, vc("x + -x == 0"), chain(expect("0 == 0", "x + -x == 0")));
        assertSimplificationSteps(simplification::apply, vc("-x + x == 0"), chain(expect("0 == 0", "-x + x == 0")));
        assertSimplificationSteps(simplification::apply, vc("x - x == 0"), chain(expect("0 == 0", "x - x == 0")));
        assertSimplificationSteps(simplification::apply, vc("--x == x"), chain(expect("x == x", "-(-x) == x")));
        assertSimplificationSteps(simplification::apply, vc("x + -y == 0"), chain(expect("x - y == 0", "x + -y == 0")));
        assertSimplificationSteps(simplification::apply, vc("x - -y == 0"), chain(expect("x + y == 0", "x - -y == 0")));
    }

    @Test
    void simplifiesMultiplicativeIdentities() {
        assertSimplificationSteps(simplification::apply, vc("x * 1 > 0"), chain(expect("x > 0", "x * 1 > 0")));
        assertSimplificationSteps(simplification::apply, vc("1 * x > 0"), chain(expect("x > 0", "1 * x > 0")));
        assertSimplificationSteps(simplification::apply, vc("x * 0 == 0"), chain(expect("0 == 0", "x * 0 == 0")));
        assertSimplificationSteps(simplification::apply, vc("0 * x == 0"), chain(expect("0 == 0", "0 * x == 0")));
        assertSimplificationSteps(simplification::apply, vc("x / 1 > 0"), chain(expect("x > 0", "x / 1 > 0")));
        assertSimplificationSteps(simplification::apply, vc("x % 1 == 0"), chain(expect("0 == 0", "x % 1 == 0")));
    }

    @Test
    void simplifiesGuardedDivisionAndModuloIdentities() {
        assertSimplificationSteps(simplification::apply, vc("x != 0", "0 / x == 0"),
                chain(expect("x != 0"), expect("0 == 0", "0 / x == 0")));
        assertSimplificationSteps(simplification::apply, vc("x != 0", "x / x == 1"),
                chain(expect("x != 0"), expect("1 == 1", "x / x == 1")));
        assertSimplificationSteps(simplification::apply, vc("0 != x", "x % x == 0"),
                chain(expect("0 != x"), expect("0 == 0", "x % x == 0")));
    }

    @Test
    void leavesUnguardedDivisionAndModuloIdentitiesUnchanged() {
        assertSimplificationSteps(simplification::apply, vc("0 / x == 0"), chain(expect("0 / x == 0")));
        assertSimplificationSteps(simplification::apply, vc("x / x == 1"), chain(expect("x / x == 1")));
        assertSimplificationSteps(simplification::apply, vc("x % x == 0"), chain(expect("x % x == 0")));
    }

    @Test
    void simplifiesOnlyFirstArithmeticIdentity() {
        assertSimplificationSteps(simplification::apply, vc("x + 0 + 1 > 0"),
                chain(expect("x + 1 > 0", "x + 0 + 1 > 0")));
    }

    @Test
    void recordsOriginWhenSimplifyingLaterImplication() {
        VCImplication implication = vc("x > 0", "y + 0 > x");

        VCImplication result = assertSimplificationSteps(simplification::apply, implication,
                chain(expect("x > 0"), expect("y > x", "y + 0 > x")));

        SimplifiedVCImplication simplifiedNext = assertInstanceOf(SimplifiedVCImplication.class, result.getNext());
        assertEquals("y + 0 > x", simplifiedNext.getOrigin().getRefinement().getExpression().toDisplayString());
    }
}
