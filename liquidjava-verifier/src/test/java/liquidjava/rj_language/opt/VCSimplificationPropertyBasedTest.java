package liquidjava.rj_language.opt;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import liquidjava.processor.VCImplication;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostFunction;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.Var;
import liquidjava.smt.SMTEvaluator;
import liquidjava.smt.SMTResult;
import liquidjava.utils.TestUtils;
import org.junit.runner.RunWith;
import spoon.Launcher;
import spoon.reflect.factory.Factory;

@RunWith(JUnitQuickcheck.class)
public class VCSimplificationPropertyBasedTest {

    private static final int TRIALS = 50; // number of random VCs to test
    private static final int MAX_STEPS = 20; // to prevent infinite loops in case of non-termination
    private static final Factory FACTORY = new Launcher().getFactory();

    @Property(trials = TRIALS)
    public void eachSimplificationStepPreservesVcSemantics(@From(VCImplicationGenerator.class) VCImplication vc) {
        setUpContext();
        VCSimplificationResult current = new VCSimplificationResult(vc);

        for (int step = 0; step < MAX_STEPS; step++) {
            VCSimplificationResult result = VCSimplification.simplifyOnce(current);
            VCImplication simplified = result.getImplication();
            if (result == current)
                return;
            assertTrue(current.getImplication().equals(result.getOrigin().getImplication()));
            assertEquivalent(current.getImplication(), simplified, step);
            current = result;
        }
        fail("VC simplification did not reach a fixed point within " + MAX_STEPS + " steps: "
                + current.getImplication());
    }

    private static void setUpContext() {
        Context.getInstance().reinitializeAllContext();
        for (String variable : VCImplicationGenerator.BINDERS)
            TestUtils.addIntVariableToContext(variable);
        for (String variable : VCImplicationGenerator.FREE_VARS)
            TestUtils.addIntVariableToContext(variable);
        for (String function : VCImplicationGenerator.FUNCTIONS)
            Context.getInstance().addGhostFunction(
                    new GhostFunction(function, List.of("int"), FACTORY.Type().INTEGER_PRIMITIVE, FACTORY, ""));
    }

    private static void assertEquivalent(VCImplication unsimplified, VCImplication simplified, int step) {
        Predicate premises = substitutionPremises(unsimplified);
        Predicate unsimplifiedFormula = Predicate.createConjunction(premises, new Predicate(vcFormula(unsimplified)));
        Predicate simplifiedFormula = Predicate.createConjunction(premises, new Predicate(vcFormula(simplified)));
        assertImplies(unsimplifiedFormula, simplifiedFormula, unsimplified, simplified, step,
                "unsimplified => simplified");
        assertImplies(simplifiedFormula, unsimplifiedFormula, unsimplified, simplified, step,
                "simplified => unsimplified");
    }

    private static Expression vcFormula(VCImplication implication) {
        Expression refinement = implication.getRefinement().getExpression().clone();
        if (!implication.hasNext())
            return refinement;
        return new BinaryExpression(refinement, "-->", vcFormula(implication.getNext()));
    }

    private static Predicate substitutionPremises(VCImplication implication) {
        Predicate premises = new Predicate();
        for (VCImplication current = implication; current != null; current = current.getNext()) {
            if (isSubstitution(current))
                premises = Predicate.createConjunction(premises, current.getRefinement());
        }
        return premises;
    }

    private static boolean isSubstitution(VCImplication implication) {
        if (!implication.hasBinder())
            return false;

        Expression refinement = implication.getRefinement().getExpression().clone();
        if (!(refinement instanceof BinaryExpression binary) || !"==".equals(binary.getOperator()))
            return false;

        String name = implication.getName();
        Expression left = binary.getFirstOperand();
        Expression right = binary.getSecondOperand();
        return isVar(left, name) && !containsVar(right, name) || isVar(right, name) && !containsVar(left, name);
    }

    private static boolean isVar(Expression expression, String name) {
        return expression instanceof Var var && name.equals(var.getName());
    }

    private static boolean containsVar(Expression expression, String name) {
        List<String> names = new ArrayList<>();
        expression.getVariableNames(names);
        return names.contains(name);
    }

    private static void assertImplies(Predicate antecedent, Predicate consequent, VCImplication unsimplified,
            VCImplication simplified, int step, String direction) {
        try {
            SMTResult result = new SMTEvaluator().verifySubtype(antecedent, consequent, Context.getInstance(), true);
            assertTrue(result.isOk(), () -> direction + " failed at step " + step + "\nunsimplified: " + unsimplified
                    + "\nsimplified: " + simplified);
        } catch (Exception e) {
            throw new AssertionError(direction + " could not be checked at step " + step + "\nunsimplified: "
                    + unsimplified + "\nsimplified: " + simplified, e);
        }
    }
}
