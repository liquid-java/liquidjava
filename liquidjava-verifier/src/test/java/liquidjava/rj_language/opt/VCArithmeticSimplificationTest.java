package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import org.junit.jupiter.api.Test;

class VCArithmeticSimplificationTest {

    private final VCArithmeticSimplification simplification = new VCArithmeticSimplification();

    @Test
    void simplifiesAdditiveIdentities() {
        assertSimplificationSteps(simplification, vc("x + 0 > 0"), step("x > 0"));
        assertSimplificationSteps(simplification, vc("0 + x > 0"), step("x > 0"));
        assertSimplificationSteps(simplification, vc("x - 0 > 0"), step("x > 0"));
        assertSimplificationSteps(simplification, vc("0 - x > 0"), step("-x > 0"));
    }

    @Test
    void simplifiesNegatedAdditionAndSubtraction() {
        assertSimplificationSteps(simplification, vc("x + -x == 0"), step("0 == 0"));
        assertSimplificationSteps(simplification, vc("-x + x == 0"), step("0 == 0"));
        assertSimplificationSteps(simplification, vc("x - x == 0"), step("0 == 0"));
        assertSimplificationSteps(simplification, vc("--x == x"), step("x == x"));
        assertSimplificationSteps(simplification, vc("x + -y == 0"), step("x - y == 0"));
        assertSimplificationSteps(simplification, vc("x - -y == 0"), step("x + y == 0"));
    }

    @Test
    void simplifiesMultiplicativeIdentities() {
        assertSimplificationSteps(simplification, vc("x * 1 > 0"), step("x > 0"));
        assertSimplificationSteps(simplification, vc("1 * x > 0"), step("x > 0"));
        assertSimplificationSteps(simplification, vc("x * 0 == 0"), step("0 == 0"));
        assertSimplificationSteps(simplification, vc("0 * x == 0"), step("0 == 0"));
        assertSimplificationSteps(simplification, vc("x / 1 > 0"), step("x > 0"));
        assertSimplificationSteps(simplification, vc("x % 1 == 0"), step("0 == 0"));
    }

    @Test
    void simplifiesGuardedDivisionAndModuloIdentities() {
        assertSimplificationSteps(simplification, vc("x != 0", "0 / x == 0"), step("x != 0", "0 == 0"));
        assertSimplificationSteps(simplification, vc("x != 0", "x / x == 1"), step("x != 0", "1 == 1"));
        assertSimplificationSteps(simplification, vc("0 != x", "x % x == 0"), step("0 != x", "0 == 0"));
    }

    @Test
    void leavesUnguardedDivisionAndModuloIdentitiesUnchanged() {
        assertSimplificationSteps(simplification, vc("0 / x == 0"), step("0 / x == 0"));
        assertSimplificationSteps(simplification, vc("x / x == 1"), step("x / x == 1"));
        assertSimplificationSteps(simplification, vc("x % x == 0"), step("x % x == 0"));
    }

    @Test
    void simplifiesOnlyFirstArithmeticIdentity() {
        assertSimplificationSteps(simplification, vc("x + 0 + 1 > 0"), step("x + 1 > 0"));
    }

    @Test
    void recordsOriginWhenSimplifyingLaterImplication() {
        assertSimplificationSteps(simplification, vc("x > 0", "y + 0 > x"), step("x > 0", "y > x"));
    }
}
