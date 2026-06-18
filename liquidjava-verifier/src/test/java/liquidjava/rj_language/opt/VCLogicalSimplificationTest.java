package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import liquidjava.processor.VCImplication;
import org.junit.jupiter.api.Test;

class VCLogicalSimplificationTest {

    private final VCLogicalSimplification simplification = new VCLogicalSimplification();

    @Test
    void simplifiesConjunctionWithBooleanLiterals() {
        assertSimplificationSteps(simplification, vc("x && true"), step("x"));
        assertSimplificationSteps(simplification, vc("true && x"), step("x"));
        assertSimplificationSteps(simplification, vc("x && false"), step("false"));
        assertSimplificationSteps(simplification, vc("false && x"), step("false"));
    }

    @Test
    void simplifiesDisjunctionWithBooleanLiterals() {
        assertSimplificationSteps(simplification, vc("x || true"), step("true"));
        assertSimplificationSteps(simplification, vc("true || x"), step("true"));
        assertSimplificationSteps(simplification, vc("x || false"), step("x"));
        assertSimplificationSteps(simplification, vc("false || x"), step("x"));
    }

    @Test
    void simplifiesDoubleNegation() {
        assertSimplificationSteps(simplification, vc("!!x"), step("x"));
    }

    @Test
    void simplifiesDuplicateLogicalOperands() {
        assertSimplificationSteps(simplification, vc("p && p"), step("p"));
        assertSimplificationSteps(simplification, vc("p || p"), step("p"));
    }

    @Test
    void simplifiesSelfEqualityAndInequality() {
        assertSimplificationSteps(simplification, vc("x == x"), step("true"));
        assertSimplificationSteps(simplification, vc("x != x"), step("false"));
    }

    @Test
    void simplifiesImplicationIdentities() {
        assertSimplificationSteps(simplification, vc("x --> true"), step("true"));
        assertSimplificationSteps(simplification, vc("false --> x"), step("true"));
        assertSimplificationSteps(simplification, vc("true --> x"), step("x"));
        assertSimplificationSteps(simplification, vc("x --> x"), step("true"));
    }

    @Test
    void simplifiesOnlyFirstLogicalIdentity() {
        assertSimplificationSteps(simplification, vc("x && true && false"), step("x && false"));
    }

    @Test
    void simplifiesNestedExpressionsBeforeParent() {
        assertSimplificationSteps(simplification, vc("(x && true) || false"), step("x || false"));
    }

    @Test
    void simplifiesIteChildren() {
        assertSimplificationSteps(simplification, vc("cond ? x && true : y || false"), step("cond ? x : y || false"));
    }

    @Test
    void recordsOriginWhenSimplifyingLaterImplication() {
        VCImplication implication = vc("x > 0", "y || false");

        assertSimplificationSteps(simplification, implication, step("x > 0", "y"));
    }
}
