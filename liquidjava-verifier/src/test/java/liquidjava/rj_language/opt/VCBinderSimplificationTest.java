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
        assertSimplificationSteps(binderSimplification, vc("∀##if_condition_1:boolean. ##if_condition_1", "y > 0"),
                step("y > 0"));
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
    void removesTrueBinderThatALaterRowShows() {
        assertSimplificationSteps(forRefinementError("y > 0"), vc("∀x:int. true", "∀y:int. y == x + 1"),
                step("y == x + 1"));
    }

    @Test
    void removesTrueBinderOfExpectedVariableThatAnotherRowShows() {
        assertSimplificationSteps(forRefinementError("x > 0"), vc("∀x:int. true", "x != 0"), step("x != 0"));
    }

    @Test
    void keepsTrueBinderOfExpectedVariableNoOtherRowMentions() {
        assertSimplificationSteps(forRefinementError("x > 0"), vc("∀x:int. true", "y > 0"), step("true", "y > 0"));
    }

    @Test
    void removesTrueBinderOfExpectedVariableInStateError() {
        assertSimplificationSteps(forStateError("x > 0"), vc("∀x:int. true", "y > 0"), step("y > 0"));
    }

    @Test
    void removesTrueBinderThatIsTheArgumentOfAGhost() {
        assertSimplificationSteps(forRefinementError("y > 0"), vc("∀k:int. true", "∀y:int. y == f(k)"),
                step("y == f(k)"));
    }

    @Test
    void removesTrueBindersThatAConditionalStateShows() {
        assertSimplificationSteps(forStateError("open(f)"),
                vc("∀ready:boolean. true", "∀f:Object. true", "ready ? open(f) : created(f)"),
                step("true", "ready ? open(f) : created(f)"), step("ready ? open(f) : created(f)"));
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
        assertSimplificationSteps(forRefinementError("x > 0"), vc("∀x:int. true", "w > 0", "∀y:int. true", "z > 0"),
                step("true", "w > 0", "z > 0"));
    }

    @Test
    void ignoresNonBinderBooleanLiterals() {
        assertSimplificationSteps(binderSimplification, vc("true", "false"), step("true", "false"));
    }

    @Test
    void removesLastTrueBinderWithoutLeavingTrue() {
        assertSimplificationSteps(binderSimplification, vc("∀p:boolean. p || q", "∀x:Object. true"), step("p || q"));
    }

    @Test
    void removesLastTrueBinderAfterNestedConditionsWithoutLeavingTrue() {
        assertSimplificationSteps(binderSimplification, vc("∀p:boolean. p", "∀q:boolean. !q", "∀x:Object. true"),
                step("p", "!q"));
    }

    @Test
    void collapsedFalseBinderLeavesNoTrueAfterAnotherRow() {
        assertSimplificationSteps(binderSimplification, vc("∀x:int. x > 0", "∀y:int. false", "z > 0"), step("x > 0"));
    }

    @Test
    void removesPathConditionsExpectedDoesNotDependOn() {
        assertSimplificationSteps(forStateError("redoable(u)"),
                vc("∀##if_condition_1:boolean. !a", "∀##if_condition_2:boolean. !(b || c)", "∀u:Object. u == g"),
                step("!(b || c)", "u == g"), step("u == g"));
    }

    @Test
    void keepsPathConditionExpectedDependsOnThroughAnotherRow() {
        assertSimplificationSteps(forRefinementError("y > 0"),
                vc("∀##if_condition_1:boolean. x > 5", "∀##if_condition_2:boolean. !c", "∀y:int. y == x - 10"),
                step("x > 5", "y == x - 10"));
    }

    @Test
    void keepsPathConditionAnotherRowMentions() {
        assertSimplificationSteps(forStateError("open(f)"),
                vc("∀##if_condition_1:boolean. p", "∀##if_condition_2:boolean. q",
                        "∀f:Object. ##if_condition_1 ? open(f) : created(f)"),
                step("p", "#if_condition ? open(f) : created(f)"));
    }

    @Test
    void keepsPathConditionsExpectedNames() {
        assertSimplificationSteps(forRefinementError("##if_condition_1 || f(1) > 1"), vc("∀##if_condition_1:boolean. p",
                "∀##if_condition_2:boolean. f(1) > 0", "∀##if_condition_3:boolean. q"), step("p", "f(1) > 0"));
    }

    @Test
    void trueBinderWithoutSuffixBecomesPlainTrue() {
        assertSimplificationSteps(binderSimplification, vc("∀x:int. true"), step("true"));
    }
}
