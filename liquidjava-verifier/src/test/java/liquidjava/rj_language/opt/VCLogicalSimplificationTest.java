package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import org.junit.jupiter.api.Test;

class VCLogicalSimplificationTest {

    private final VCLogicalSimplification simplification = new VCLogicalSimplification();

    @Test
    void simplifiesConjunctionWithBooleanLiterals() {
        assertSimplificationSteps(simplification::apply, vc("x && true"), chain(expect("x", "x && true")));
        assertSimplificationSteps(simplification::apply, vc("true && x"), chain(expect("x", "true && x")));
        assertSimplificationSteps(simplification::apply, vc("x && false"), chain(expect("false", "x && false")));
        assertSimplificationSteps(simplification::apply, vc("false && x"), chain(expect("false", "false && x")));
    }

    @Test
    void simplifiesDisjunctionWithBooleanLiterals() {
        assertSimplificationSteps(simplification::apply, vc("x || true"), chain(expect("true", "x || true")));
        assertSimplificationSteps(simplification::apply, vc("true || x"), chain(expect("true", "true || x")));
        assertSimplificationSteps(simplification::apply, vc("x || false"), chain(expect("x", "x || false")));
        assertSimplificationSteps(simplification::apply, vc("false || x"), chain(expect("x", "false || x")));
    }

    @Test
    void simplifiesDoubleNegation() {
        assertSimplificationSteps(simplification::apply, vc("!!x"), chain(expect("x", "!!x")));
    }

    @Test
    void simplifiesDuplicateLogicalOperands() {
        assertSimplificationSteps(simplification::apply, vc("p && p"), chain(expect("p", "p && p")));
        assertSimplificationSteps(simplification::apply, vc("p || p"), chain(expect("p", "p || p")));
    }

    @Test
    void simplifiesSelfEqualityAndInequality() {
        assertSimplificationSteps(simplification::apply, vc("x == x"), chain(expect("true", "x == x")));
        assertSimplificationSteps(simplification::apply, vc("x != x"), chain(expect("false", "x != x")));
    }

    @Test
    void simplifiesImplicationIdentities() {
        assertSimplificationSteps(simplification::apply, vc("x --> true"), chain(expect("true", "x --> true")));
        assertSimplificationSteps(simplification::apply, vc("false --> x"), chain(expect("true", "false --> x")));
        assertSimplificationSteps(simplification::apply, vc("true --> x"), chain(expect("x", "true --> x")));
        assertSimplificationSteps(simplification::apply, vc("x --> x"), chain(expect("true", "x --> x")));
    }

    @Test
    void simplifiesOnlyFirstLogicalIdentity() {
        assertSimplificationSteps(simplification::apply, vc("x && true && false"),
                chain(expect("x && false", "x && true && false")));
    }

    @Test
    void simplifiesNestedExpressionsBeforeParent() {
        assertSimplificationSteps(simplification::apply, vc("(x && true) || false"),
                chain(expect("x || false", "x && true || false")));
    }

    @Test
    void simplifiesIteChildren() {
        assertSimplificationSteps(simplification::apply, vc("cond ? x && true : y || false"),
                chain(expect("cond ? x : y || false", "cond ? x && true : y || false")));
    }

    @Test
    void recordsOriginWhenSimplifyingLaterImplication() {
        VCImplication implication = vc("x > 0", "y || false");

        VCImplication result = assertSimplificationSteps(simplification::apply, implication,
                chain(expect("x > 0", "x > 0"), expect("y", "y || false")));

        SimplifiedVCImplication simplifiedNext = assertInstanceOf(SimplifiedVCImplication.class, result.getNext());
        assertEquals("y || false", simplifiedNext.getOrigin().getRefinement().getExpression().toDisplayString());
    }
}
