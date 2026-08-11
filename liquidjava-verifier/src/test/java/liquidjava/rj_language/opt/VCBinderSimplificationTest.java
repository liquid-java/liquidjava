package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import org.junit.jupiter.api.Test;

class VCBinderSimplificationTest {

    private final VCBinderSimplification binderSimplification = new VCBinderSimplification();

    @Test
    void removesTrueBinderWhenVariableIsUnusedDownstream() {
        assertSimplificationSteps(binderSimplification, vc("∀x:int. true", "y > 0"), step("y > 0"));
    }

    @Test
    void removesFreshPathBinderWhenVariableIsUnusedDownstream() {
        assertSimplificationSteps(binderSimplification, vc("∀#fresh_1:boolean. #fresh_1", "y > 0"), step("y > 0"));
    }

    @Test
    void keepsNonTrueBinderWhenVariableIsUnusedDownstream() {
        assertSimplificationSteps(binderSimplification, vc("∀x:int. x > 0", "y > 0"), step("x > 0", "y > 0"));
    }

    @Test
    void keepsNonTrueTerminalBinderAsConclusion() {
        assertSimplificationSteps(binderSimplification, vc("∀x:int. x > 0"), step("x > 0"));
    }

    @Test
    void keepsTrueBinderWhenVariableIsUsedDownstream() {
        assertSimplificationSteps(binderSimplification, vc("∀x:int. true", "x > 0"), step("true", "x > 0"));
    }

    @Test
    void collapsesFalseBinderSuffixToPlainTrue() {
        assertSimplificationSteps(binderSimplification, vc("∀x:int. false", "y > 0"), step("true"));
    }

    @Test
    void simplifiesOnlyFirstApplicableBinder() {
        assertSimplificationSteps(binderSimplification, vc("∀x:int. true", "∀y:int. true", "z > 0"),
                step("true", "z > 0"));
    }

    @Test
    void skipsInapplicableTrueBinderAndSimplifiesLaterBinder() {
        assertSimplificationSteps(binderSimplification, vc("∀x:int. true", "x > 0", "∀y:int. true", "z > 0"),
                step("true", "x > 0", "z > 0"));
    }

    @Test
    void ignoresNonBinderBooleanLiterals() {
        assertSimplificationSteps(binderSimplification, vc("true", "false"), step("true", "false"));
    }

    @Test
    void trueBinderWithoutSuffixBecomesPlainTrue() {
        assertSimplificationSteps(binderSimplification, vc("∀x:int. true"), step("true"));
    }
}
