package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.assertSimplificationSteps;
import static liquidjava.utils.VCTestUtils.step;
import static liquidjava.utils.VCTestUtils.vc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import liquidjava.processor.context.Context;
import liquidjava.utils.TestUtils;

class VCConstraintSimplificationTest {

    private final VCConstraintSimplification simplification = new VCConstraintSimplification();

    @BeforeEach
    void setUpContext() {
        Context.getInstance().reinitializeAllContext();
        TestUtils.addIntVariableToContext("x");
        TestUtils.addIntVariableToContext("y");
    }

    @AfterEach
    void resetContext() {
        Context.getInstance().reinitializeAllContext();
    }

    @Test
    void keepsRedundantConstraintWhenItsBinderIsRequired() {
        assertSimplificationSteps(simplification,
                vc("∀x:int. x > 0", "∀cond:boolean. x > 1", "∀y:int. y == x + 1", "y < 0"),
                step("x > 0", "x > 1", "y == x + 1", "y < 0"));
    }

    @Test
    void removesConstraintImpliedByEarlierAntecedent() {
        assertSimplificationSteps(simplification, vc("∀x:int. x > 1", "∀cond:boolean. x > 0", "∀y:int. y == x + 1"),
                step("x > 1", "y == x + 1"));
    }

    @Test
    void removesCoverageConstraintsImpliedByStrongerLaterConstraints() {
        assertSimplificationSteps(simplification,
                vc("∀x:int. x >= 0", "∀#fresh_40:boolean. !(y < 40)", "∀#fresh_60:boolean. !(y < 60)",
                        "∀#fresh_80:boolean. !(y < 80)", "x + y > 0"),
                step("x >= 0", "!(y < 60)", "!(y < 80)", "x + y > 0"), step("x >= 0", "!(y < 80)", "x + y > 0"));
    }

    @Test
    void keepsConstraintsWhenBothBindersAreRequired() {
        assertSimplificationSteps(simplification, vc("∀x:int. y >= 0", "∀y:int. y > 0", "x + y > 0"),
                step("y >= 0", "y > 0", "x + y > 0"));
    }

    @Test
    void keepsConstraintsThatDoNotImplyEachOther() {
        assertSimplificationSteps(simplification, vc("∀x:int. x > 1", "y > 0", "x + y > 0"),
                step("x > 1", "y > 0", "x + y > 0"));
    }

    @Test
    void ignoresUnrelatedLaterAntecedent() {
        assertSimplificationSteps(simplification, vc("∀x:int. x > 0", "y > 1", "x + y > 0"),
                step("x > 0", "y > 1", "x + y > 0"));
    }

    @Test
    void doesNotUseConclusionToSimplifyConstraint() {
        assertSimplificationSteps(simplification, vc("∀x:int. x > 0", "x > 1"), step("x > 0", "x > 1"));
    }

    @Test
    void simplifiesOnlyFirstImpliedConstraint() {
        assertSimplificationSteps(simplification,
                vc("∀x:int. x > 2", "∀#fresh_1:boolean. x > 1", "∀#fresh_2:boolean. x > 0", "y > 0"),
                step("x > 2", "x > 0", "y > 0"), step("x > 2", "y > 0"));
    }
}
