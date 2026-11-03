package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.parsing.RefinementsParser;
import spoon.Launcher;
import spoon.reflect.factory.TypeFactory;

class VCBinderSimplificationTest {

    private final VCBinderSimplification binderSimplification = new VCBinderSimplification();

    private static final TypeFactory TYPES = new Launcher().getFactory().Type();

    private static VCBinderSimplification forRefinementError(String... expected) {
        return new VCBinderSimplification(new NeededVariables(Set.of(expected), Set.of(expected), true));
    }

    private static VCBinderSimplification forStateError(String... expected) {
        return new VCBinderSimplification(new NeededVariables(Set.of(expected), Set.of(expected), false));
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
    void removesTrueFieldBinderUsedDownstreamWhenNothingNeedsIt() {
        VCImplication field = new VCImplication("this#shop.Basket.items", TYPES.INTEGER_PRIMITIVE, new Predicate());
        field.setNext(new VCImplication("#bound_6", TYPES.INTEGER_PRIMITIVE,
                Predicate.createEquals(Predicate.createVar("#bound_6"), Predicate.createOperation(
                        Predicate.createVar("this#shop.Basket.items"), "+", Predicate.createLit("1", "int")))));
        assertSimplificationSteps(binderSimplification, field, step("bound⁶ == this.items + 1"));
    }

    @Test
    void keepsTrueBinderLinkedToExpectedThroughALaterRow() {
        assertSimplificationSteps(forRefinementError("y"), vc("∀x:int. true", "∀y:int. y == x + 1"),
                step("true", "y == x + 1"));
    }

    @Test
    void keepsTrueBinderLinkedToExpectedThroughSeveralRows() {
        assertSimplificationSteps(forRefinementError("z"), vc("∀x:int. true", "∀y:int. y == x + 1", "∀z:int. z == y"),
                step("true", "y == x + 1", "z == y"));
    }

    @Test
    void removesTrueBinderUsedOnlyByARowUnlinkedToExpected() {
        assertSimplificationSteps(forRefinementError("t"), vc("∀port:int. true", "!(port < 0)", "∀t:int. true"),
                step("!(port < 0)", "true"));
    }

    @Test
    void splitsANegatedDisjunctionSoOneGuardDoesNotLinkItsParts() {
        assertSimplificationSteps(forRefinementError("t"), vc("∀p:boolean. true", "∀t:int. true", "!(p || t < 0)"),
                step("true", "!(p || t < 0)"));
    }

    @Test
    void keepsTrueBinderOfExpectedVariableInRefinementError() {
        assertSimplificationSteps(forRefinementError("x"), vc("∀x:int. true", "y > 0"), step("true", "y > 0"));
    }

    @Test
    void keepsTerminalTrueBinderOfExpectedVariableInRefinementError() {
        VCSimplificationResult result = assertSimplificationSteps(forRefinementError("x"), vc("∀x:int. true"),
                step("true"));
        assertEquals(List.of("x"), result.getBinders());
    }

    @Test
    void removesTrueBinderOfExpectedVariableInStateError() {
        assertSimplificationSteps(forStateError("x"), vc("∀x:int. true", "y > 0"), step("y > 0"));
    }

    @Test
    void keepsTrueBinderLinkedToExpectedInStateError() {
        assertSimplificationSteps(forStateError("f"), vc("∀ready:boolean. true", "∀f:int. ready ? f > 0 : f < 0"),
                step("true", "ready ? f > 0 : f < 0"));
    }

    @Test
    void removesOnlyTheTrueBindersNothingLinks() {
        assertSimplificationSteps(forRefinementError("y"),
                vc("∀a:int. true", "∀c:int. true", "∀w:int. w > a", "∀y:int. y == c"), step("true", "w > a", "y == c"));
    }

    @Test
    void movesKeptTrueBinderBeforeItsFirstUse() {
        VCSimplificationResult result = assertSimplificationSteps(forRefinementError("i"),
                vc("∀y:int. y < i", "∀i:int. true"), step("true", "y < i"));
        assertEquals(List.of("i", "y"), result.getBinders());
    }

    @Test
    void removesTrueBinderNamedOnlyAsTheObjectOfAGhost() {
        assertSimplificationSteps(forRefinementError("b"), vc("∀x:Object. true", "∀b:int. b == size(x) && b >= 0"),
                step("b == size(x) && b >= 0"));
    }

    @Test
    void keepsPrimitiveTrueBinderThatIsTheArgumentOfAGhost() {
        assertSimplificationSteps(forRefinementError("y"), vc("∀k:int. true", "∀y:int. y == f(k)"),
                step("true", "y == f(k)"));
    }

    @Test
    void linksThroughGhostApplicationsButKeepsOnlyValueVariables() {
        assertSimplificationSteps(forStateError("open(f)", "f"),
                vc("∀ready:boolean. true", "∀f:int. true", "ready ? open(f) : created(f)"),
                step("true", "ready ? open(f) : created(f)"));
    }

    @Test
    void neededVariablesComeFromExpected() {
        Predicate expected = new Predicate(RefinementsParser.createAST("x > size(y)", ""));
        assertEquals(new NeededVariables(Set.of("x", "y", "size(y)"), Set.of("x"), true),
                NeededVariables.forRefinementError(expected));
        assertEquals(new NeededVariables(Set.of("x", "y", "size(y)"), Set.of("x"), false),
                NeededVariables.forStateError(expected));
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
        assertSimplificationSteps(forRefinementError("x"), vc("∀x:int. true", "x > 0", "∀y:int. true", "z > 0"),
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
