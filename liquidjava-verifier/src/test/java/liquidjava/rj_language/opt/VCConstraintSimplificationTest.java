package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.assertSimplificationSteps;
import static liquidjava.utils.VCTestUtils.step;
import static liquidjava.utils.VCTestUtils.vc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import liquidjava.processor.VCImplication;
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
    void simplifiesConstraintImpliedByLaterAntecedent() {
        assertSimplificationSteps(simplification,
                vc("∀x:int. x > 0", "∀cond:boolean. x > 1", "∀y:int. y == x + 1", "y < 0"),
                step("true", "x > 1", "y == x + 1", "y < 0"));
    }

    @Test
    void preservesBothBindersWhenSimplifyingConstraint() {
        VCImplication simplified = simplification
                .apply(vc("∀x:int. x > 0", "∀cond:boolean. x > 1", "∀y:int. y == x + 1", "y < 0"));

        assertTrue(simplified.hasBinder());
        assertEquals("x", simplified.getName());
        assertEquals("int", simplified.getType().getQualifiedName());
        assertTrue(simplified.getNext().hasBinder());
        assertEquals("cond", simplified.getNext().getName());
        assertEquals("boolean", simplified.getNext().getType().getQualifiedName());
    }

    @Test
    void keepsConstraintThatIsNotImpliedByLaterAntecedent() {
        assertSimplificationSteps(simplification, vc("∀x:int. x > 1", "x > 0", "y > 0"),
                step("x > 1", "x > 0", "y > 0"));
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
        assertSimplificationSteps(simplification, vc("∀x:int. x > 0", "x > 1", "x > 2", "y > 0"),
                step("true", "x > 1", "x > 2", "y > 0"), step("true", "x > 2", "y > 0"));
    }
}
