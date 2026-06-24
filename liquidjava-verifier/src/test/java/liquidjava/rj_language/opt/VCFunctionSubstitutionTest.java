package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;

import liquidjava.processor.VCImplication;
import org.junit.jupiter.api.Test;

class VCFunctionSubstitutionTest {

    private final VCFunctionSubstitution substitution = new VCFunctionSubstitution();

    @Test
    void substitutesExactFunctionInvocationIntoSuffix() {
        VCImplication implication = vc("f(x) == 0", "f(y) == f(x) + 1");

        assertSimplificationSteps(substitution::apply, implication, step("f(y) == 0 + 1"));
    }

    @Test
    void substitutesReverseFunctionEquality() {
        VCImplication implication = vc("0 == f(x)", "f(y) == f(x) + 1");

        assertSimplificationSteps(substitution::apply, implication, step("f(y) == 0 + 1"));
    }

    @Test
    void consumesSourceNodeWhenSubstitutedInvocationIsGoneFromSuffix() {
        VCImplication implication = vc("f(x) == 0", "f(x) > -1");

        assertSimplificationSteps(substitution::apply, implication, step("0 > -1"));
    }

    @Test
    void doesNotRewriteEarlierNodesFromLaterEquality() {
        VCImplication implication = vc("f(x) > 0", "f(x) == 1");

        assertSimplificationSteps(substitution::apply, implication, step("f(x) > 0", "f(x) == 1"));
    }

    @Test
    void skipsUsedUpEqualityAndUsesNextAvailableEquality() {
        VCImplication implication = vc("f(x) == 0", "f(y) == f(x) + 1", "f(y) == 1");

        assertSimplificationSteps(substitution::apply, implication, step("f(y) == 0 + 1", "f(y) == 1"),
                step("0 + 1 == 1"));
    }

    @Test
    void doesNotGeneralizeAcrossDifferentArguments() {
        VCImplication implication = vc("f(x) == 0", "f(y) == 0");

        assertSimplificationSteps(substitution::apply, implication, step("f(x) == 0", "f(y) == 0"));
    }

    @Test
    void ignoresRecursiveFunctionEquality() {
        VCImplication implication = vc("f(x) == f(x) + 1", "f(x) > 0");

        assertSimplificationSteps(substitution::apply, implication, step("f(x) == f(x) + 1", "f(x) > 0"));
    }

    @Test
    void extractsEqualityFromTopLevelConjunction() {
        VCImplication implication = vc("ok && f(x) == 0", "f(y) == f(x) + 1");

        assertSimplificationSteps(substitution::apply, implication, step("ok", "f(y) == 0 + 1"));
    }

    @Test
    void removesOnlySourceEqualityConjunct() {
        VCImplication implication = vc("ok && f(x) == 0 && ready", "f(y) == f(x) + 1");

        assertSimplificationSteps(substitution::apply, implication, step("ok && ready", "f(y) == 0 + 1"));
    }
}
