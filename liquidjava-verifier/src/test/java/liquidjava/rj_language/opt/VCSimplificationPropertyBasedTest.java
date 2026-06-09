package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSubstitution.activeExpression;
import static liquidjava.rj_language.opt.VCSubstitution.containsVariable;
import static liquidjava.rj_language.opt.VCSubstitution.isVar;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import liquidjava.processor.VCImplication;
import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.smt.SMTEvaluator;
import liquidjava.smt.SMTResult;
import liquidjava.utils.TestUtils;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class VCSimplificationPropertyBasedTest {

    private static final int TRIALS = 500;

    @Property(trials = TRIALS)
    public void eachSimplificationStepPreservesVcSemantics(@From(VCImplicationGenerator.class) VCImplication vc) {
        setUpContext();
        VCImplication current = vc;

        for (int step = 0; step < VCImplicationGenerator.BINDERS.length; step++) {
            VCImplication simplified = VCSimplifier.simplifyOnce(current);
            if (sameVc(current, simplified))
                break;

            assertEquivalent(current, simplified, step);
            current = simplified;
        }
        // System.out.println("---------------------------------------------------------");
    }

    private static void setUpContext() {
        Context.getInstance().reinitializeAllContext();
        for (String variable : VCImplicationGenerator.BINDERS)
            TestUtils.addIntVariableToContext(variable);
        for (String variable : VCImplicationGenerator.FREE_VARS)
            TestUtils.addIntVariableToContext(variable);
    }

    private static boolean sameVc(VCImplication left, VCImplication right) {
        return left.toString().equals(right.toString());
    }

    private static void assertEquivalent(VCImplication unsimplified, VCImplication simplified, int step) {
        Predicate premises = substitutionPremises(unsimplified);
        Predicate unsimplifiedFormula = Predicate.createConjunction(premises, new Predicate(vcFormula(unsimplified)));
        Predicate simplifiedFormula = Predicate.createConjunction(premises, new Predicate(vcFormula(simplified)));
        // System.out.println(unsimplifiedFormula);
        // System.out.println("=>");
        // System.out.println(simplifiedFormula);
        assertImplies(unsimplifiedFormula, simplifiedFormula, unsimplified, simplified, step,
                "unsimplified => simplified");
        assertImplies(simplifiedFormula, unsimplifiedFormula, unsimplified, simplified, step,
                "simplified => unsimplified");
    }

    private static Expression vcFormula(VCImplication implication) {
        Expression refinement = activeExpression(implication.getRefinement()).clone();
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

        Expression refinement = activeExpression(implication.getRefinement());
        if (!(refinement instanceof BinaryExpression binary) || !"==".equals(binary.getOperator()))
            return false;

        String name = implication.getName();
        Expression left = binary.getFirstOperand();
        Expression right = binary.getSecondOperand();
        return isVar(left, name) && !containsVariable(right, name)
                || isVar(right, name) && !containsVariable(left, name);
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
