package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

import liquidjava.processor.VCImplication;
import org.junit.jupiter.api.Test;

class VCBinderSimplificationTest {

    private final VCBinderSimplification binderSimplification = new VCBinderSimplification();

    @Test
    void removesTrueBinderWhenVariableIsUnusedDownstream() {
        VCImplication implication = vc("∀x:int. true", "y > 0");

        assertSimplificationSteps(binderSimplification, implication, step("y > 0"));
    }

    @Test
    void keepsTrueBinderWhenVariableIsUsedDownstream() {
        VCImplication implication = vc("∀x:int. true", "x > 0");

        assertSimplificationSteps(binderSimplification, implication, step("true", "x > 0"));
    }

    @Test
    void collapsesFalseBinderSuffixToPlainTrue() {
        VCImplication implication = vc("∀x:int. false", "x > 0", "y > 0");
        VCImplication result = binderSimplification.apply(implication);

        assertFalse(result.hasBinder());
        assertSimplifiedVC(result, "true");
    }

    @Test
    void simplifiesOnlyFirstApplicableBinder() {
        VCImplication implication = vc("∀x:int. true", "∀y:int. true", "z > 0");

        assertSimplificationSteps(binderSimplification, implication, step("true", "z > 0"));
    }

    @Test
    void skipsInapplicableTrueBinderAndSimplifiesLaterBinder() {
        VCImplication implication = vc("∀x:int. true", "x > 0", "∀y:int. true", "z > 0");

        assertSimplificationSteps(binderSimplification, implication, step("true", "x > 0", "z > 0"));
    }

    @Test
    void ignoresNonBinderBooleanLiterals() {
        VCImplication implication = vc("true", "false");

        assertSimplificationSteps(binderSimplification, implication, step("true", "false"));
    }

    @Test
    void trueBinderWithoutSuffixBecomesPlainTrue() {
        VCImplication implication = vc("∀x:int. true");
        VCImplication result = binderSimplification.apply(implication);

        assertFalse(result.hasBinder());
        assertSimplifiedVC(result, "true");
    }
}
