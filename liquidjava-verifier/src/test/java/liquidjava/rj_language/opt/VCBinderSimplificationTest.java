package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.parsing.RefinementsParser;
import liquidjava.smt.Counterexample;
import liquidjava.utils.Pair;
import spoon.Launcher;
import spoon.reflect.factory.TypeFactory;

class VCBinderSimplificationTest {

    private final VCBinderSimplification binderSimplification = new VCBinderSimplification();

    private static final TypeFactory TYPES = new Launcher().getFactory().Type();

    private static VCBinderSimplification binderWithNeeds(Set<String> expected, Set<String> assigned) {
        return new VCBinderSimplification(new NeededVariables(expected, assigned));
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
    void keepsTrueBinderUsedDownstreamWhenCounterexampleAssignsIt() {
        assertSimplificationSteps(binderWithNeeds(Set.of(), Set.of("x")), vc("∀x:int. true", "x > 0"),
                step("true", "x > 0"));
    }

    @Test
    void removesTrueBinderAssignedByCounterexampleWhenUnusedDownstream() {
        assertSimplificationSteps(binderWithNeeds(Set.of(), Set.of("x")), vc("∀x:int. true", "y > 0"), step("y > 0"));
    }

    @Test
    void keepsTrueBinderOfExpectedVariable() {
        assertSimplificationSteps(binderWithNeeds(Set.of("x"), Set.of()), vc("∀x:int. true", "y > 0"),
                step("true", "y > 0"));
    }

    @Test
    void keepsTerminalTrueBinderOfExpectedVariable() {
        VCSimplificationResult result = assertSimplificationSteps(binderWithNeeds(Set.of("x"), Set.of()),
                vc("∀x:int. true"), step("true"));
        assertEquals(List.of("x"), result.getBinders());
    }

    @Test
    void removesOnlyTheTrueBindersNothingNeeds() {
        assertSimplificationSteps(binderWithNeeds(Set.of("e"), Set.of("c")),
                vc("∀a:int. true", "∀c:int. true", "∀e:int. true", "∀y:int. y == a + c", "y > e"),
                step("true", "true", "y == a + c", "y > e"));
    }

    @Test
    void neededVariablesComeFromExpectedAndCounterexample() {
        NeededVariables needed = NeededVariables.of(new Predicate(RefinementsParser.createAST("x > y", "")),
                new Counterexample(List.of(new Pair<>("z", "0"))));
        assertEquals(Set.of("x", "y"), needed.expected());
        assertEquals(Set.of("z"), needed.assigned());
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
        assertSimplificationSteps(binderWithNeeds(Set.of(), Set.of("x")),
                vc("∀x:int. true", "x > 0", "∀y:int. true", "z > 0"), step("true", "x > 0", "z > 0"));
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
