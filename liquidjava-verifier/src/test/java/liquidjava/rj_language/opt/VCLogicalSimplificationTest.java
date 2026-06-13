package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import org.junit.jupiter.api.Test;

class VCLogicalSimplificationTest {

    @Test
    void applyReturnsNullForNullImplication() {
        assertNull(VCLogicalSimplification.apply(null));
    }

    @Test
    void simplifiesConjunctionWithBooleanLiterals() {
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("x && true"), chain(expect("x", "x && true")));
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("true && x"), chain(expect("x", "true && x")));
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("x && false"),
                chain(expect("false", "x && false")));
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("false && x"),
                chain(expect("false", "false && x")));
    }

    @Test
    void simplifiesDisjunctionWithBooleanLiterals() {
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("x || true"), chain(expect("true", "x || true")));
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("true || x"), chain(expect("true", "true || x")));
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("x || false"), chain(expect("x", "x || false")));
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("false || x"), chain(expect("x", "false || x")));
    }

    @Test
    void simplifiesDoubleNegation() {
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("!!x"), chain(expect("x", "!!x")));
    }

    @Test
    void simplifiesDuplicateLogicalOperands() {
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("p && p"), chain(expect("p", "p && p")));
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("p || p"), chain(expect("p", "p || p")));
    }

    @Test
    void simplifiesSelfEqualityAndInequality() {
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("x == x"), chain(expect("true", "x == x")));
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("x != x"), chain(expect("false", "x != x")));
    }

    @Test
    void simplifiesImplicationIdentities() {
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("x --> true"),
                chain(expect("true", "x --> true")));
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("false --> x"),
                chain(expect("true", "false --> x")));
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("true --> x"), chain(expect("x", "true --> x")));
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("x --> x"), chain(expect("true", "x --> x")));
    }

    @Test
    void simplifiesOnlyFirstLogicalIdentity() {
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("x && true && false"),
                chain(expect("x && false", "x && true && false")));
    }

    @Test
    void simplifiesNestedExpressionsBeforeParent() {
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("(x && true) || false"),
                chain(expect("x || false", "x && true || false")));
    }

    @Test
    void simplifiesIteChildren() {
        assertSimplificationSteps(VCLogicalSimplification::apply, vc("cond ? x && true : y || false"),
                chain(expect("cond ? x : y || false", "cond ? x && true : y || false")));
    }

    @Test
    void recordsOriginWhenSimplifyingLaterImplication() {
        VCImplication implication = vc("x > 0", "y || false");

        VCImplication result = assertSimplificationSteps(VCLogicalSimplification::apply, implication,
                chain(expect("x > 0", "x > 0"), expect("y", "y || false")));

        SimplifiedVCImplication simplifiedNext = assertInstanceOf(SimplifiedVCImplication.class, result.getNext());
        assertEquals("y || false", simplifiedNext.getOrigin().getRefinement().toString());
    }

    @Test
    void preservesOriginFromExistingSimplifiedImplication() {
        VCImplication substituted = VCSubstitution.apply(vc("∀x:int. x == y", "x == x"));

        assertSimplificationSteps(VCLogicalSimplification::apply, substituted, chain(expect("true", "∀x:int. x == x")));
    }
}
