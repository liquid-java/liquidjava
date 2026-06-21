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

        assertSimplificationSteps(binderSimplification::apply, implication, chain(expect("y > 0", "∀x:int. y > 0")));
    }

    @Test
    void keepsTrueBinderWhenVariableIsUsedDownstream() {
        VCImplication implication = vc("∀x:int. true", "x > 0");

        assertSimplificationSteps(binderSimplification::apply, implication,
                chain(expect("true", "∀x:int. true"), expect("x > 0", "x > 0")));
    }

    @Test
    void collapsesFalseBinderSuffixToPlainTrue() {
        VCImplication implication = vc("∀x:int. false", "x > 0", "y > 0");
        VCImplication result = binderSimplification.apply(implication);

        assertFalse(result.hasBinder());
        assertSimplifiedVC(result, expect("true", "∀x:int. false"));
    }

    @Test
    void simplifiesOnlyFirstApplicableBinder() {
        VCImplication implication = vc("∀x:int. true", "∀y:int. true", "z > 0");

        assertSimplificationSteps(binderSimplification::apply, implication,
                chain(expect("true", "∀x:int. true"), expect("z > 0", "z > 0")));
    }

    @Test
    void skipsInapplicableTrueBinderAndSimplifiesLaterBinder() {
        VCImplication implication = vc("∀x:int. true", "x > 0", "∀y:int. true", "z > 0");

        assertSimplificationSteps(binderSimplification::apply, implication,
                chain(expect("true", "∀x:int. true"), expect("x > 0", "x > 0"), expect("z > 0", "∀y:int. z > 0")));
    }

    @Test
    void ignoresNonBinderBooleanLiterals() {
        VCImplication implication = vc("true", "false");

        assertSimplificationSteps(binderSimplification::apply, implication,
                chain(expect("true", "true"), expect("false", "false")));
    }

    @Test
    void trueBinderWithoutSuffixBecomesPlainTrue() {
        VCImplication implication = vc("∀x:int. true");
        VCImplication result = binderSimplification.apply(implication);

        assertFalse(result.hasBinder());
        assertSimplifiedVC(result, expect("true", "∀x:int. true"));
    }
}
