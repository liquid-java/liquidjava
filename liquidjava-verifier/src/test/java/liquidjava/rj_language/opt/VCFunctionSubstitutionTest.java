package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;

import liquidjava.processor.VCImplication;
import org.junit.jupiter.api.Test;

class VCFunctionSubstitutionTest {

    private final VCFunctionSubstitution substitution = new VCFunctionSubstitution();

    @Test
    void substitutesExactFunctionInvocationIntoSuffix() {
        VCImplication implication = vc("f(x) == 0", "f(y) == f(x) + 1");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("f(x) == 0"), expect("f(y) == 0 + 1", "f(x) == 0")));
    }

    @Test
    void substitutesReverseFunctionEquality() {
        VCImplication implication = vc("0 == f(x)", "f(y) == f(x) + 1");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("0 == f(x)"), expect("f(y) == 0 + 1", "0 == f(x)")));
    }

    @Test
    void preservesSourceNode() {
        VCImplication implication = vc("f(x) == 0", "f(x) > -1");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("f(x) == 0"), expect("0 > -1", "f(x) == 0")));
    }

    @Test
    void doesNotRewriteEarlierNodesFromLaterEquality() {
        VCImplication implication = vc("f(x) > 0", "f(x) == 1");

        assertSimplificationSteps(substitution::apply, implication, chain(expect("f(x) > 0"), expect("f(x) == 1")));
    }

    @Test
    void skipsUsedUpEqualityAndUsesNextAvailableEquality() {
        VCImplication implication = vc("f(x) == 0", "f(y) == f(x) + 1", "f(y) == 1");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("f(x) == 0"), expect("f(y) == 0 + 1", "f(x) == 0"), expect("f(y) == 1")),
                chain(expect("f(x) == 0"), expect("f(y) == 0 + 1", "f(x) == 0"),
                        expect("0 + 1 == 1", "f(y) == 0 + 1")));
    }

    @Test
    void doesNotGeneralizeAcrossDifferentArguments() {
        VCImplication implication = vc("f(x) == 0", "f(y) == 0");

        assertSimplificationSteps(substitution::apply, implication, chain(expect("f(x) == 0"), expect("f(y) == 0")));
    }

    @Test
    void ignoresRecursiveFunctionEquality() {
        VCImplication implication = vc("f(x) == f(x) + 1", "f(x) > 0");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("f(x) == f(x) + 1"), expect("f(x) > 0")));
    }

    @Test
    void extractsEqualityFromTopLevelConjunction() {
        VCImplication implication = vc("ok && f(x) == 0", "f(y) == f(x) + 1");

        assertSimplificationSteps(substitution::apply, implication,
                chain(expect("ok && f(x) == 0"), expect("f(y) == 0 + 1", "ok && f(x) == 0")));
    }
}
