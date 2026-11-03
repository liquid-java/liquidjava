package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;

import org.junit.jupiter.api.Test;

import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.parsing.RefinementsParser;

class VCBinderSimplificationTest {

    private final VCBinderSimplification binderSimplification = new VCBinderSimplification();

    private static VCBinderSimplification forRefinementError(String expected) {
        return new VCBinderSimplification(NeededVariables.forRefinementError(predicate(expected)));
    }

    private static VCBinderSimplification forStateError(String expected) {
        return new VCBinderSimplification(NeededVariables.forStateError(predicate(expected)));
    }

    private static Predicate predicate(String refinement) {
        return new Predicate(RefinementsParser.createAST(refinement, ""));
    }

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
    void removesTrueBinderUsedDownstreamWhenNothingNeedsIt() {
        assertSimplificationSteps(binderSimplification, vc("∀x:int. true", "x > 0"), step("x > 0"));
    }

    @Test
    void keepsTrueBinderLinkedToExpectedThroughALaterRow() {
        assertSimplificationSteps(forRefinementError("y > 0"), vc("∀x:int. true", "∀y:int. y == x + 1"),
                step("true", "y == x + 1"));
    }

    @Test
    void keepsTrueBinderLinkedToExpectedThroughSeveralRows() {
        assertSimplificationSteps(forRefinementError("z > 0"),
                vc("∀x:int. true", "∀y:int. y == x + 1", "∀z:int. z == y"), step("true", "y == x + 1", "z == y"));
    }

    @Test
    void removesTrueBindersOnlyAnUnlinkedGuardUses() {
        // one guard !(p || t < 0) states !p and !(t < 0) apart, so only the second is linked to t
        assertSimplificationSteps(forRefinementError("t > 0"),
                vc("∀port:int. true", "∀p:boolean. true", "!(port < 0)", "!(p || t < 0)", "∀t:int. true"),
                step("true", "!(port < 0)", "!(p || t < 0)", "true"), step("!(port < 0)", "!(p || t < 0)", "true"));
    }

    @Test
    void keepsTrueBinderOfExpectedVariableInRefinementError() {
        assertSimplificationSteps(forRefinementError("x > 0"), vc("∀x:int. true", "y > 0"), step("true", "y > 0"));
    }

    @Test
    void removesTrueBinderOfExpectedVariableInStateError() {
        assertSimplificationSteps(forStateError("x > 0"), vc("∀x:int. true", "y > 0"), step("y > 0"));
    }

    @Test
    void keepsPrimitiveTrueBinderThatIsTheArgumentOfAGhost() {
        assertSimplificationSteps(forRefinementError("y > 0"), vc("∀k:int. true", "∀y:int. y == f(k)"),
                step("true", "y == f(k)"));
    }

    @Test
    void linksThroughGhostApplicationsButKeepsOnlyValueVariables() {
        assertSimplificationSteps(forStateError("open(f)"),
                vc("∀ready:boolean. true", "∀f:Object. true", "ready ? open(f) : created(f)"),
                step("true", "ready ? open(f) : created(f)"));
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
        assertSimplificationSteps(forRefinementError("x > 0"), vc("∀x:int. true", "x > 0", "∀y:int. true", "z > 0"),
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
