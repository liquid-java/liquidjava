package liquidjava.smt;

import com.martiansoftware.jsap.SyntaxException;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;

import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;

public class SMTEvaluator {

    /**
     * Verifies that subRef is a subtype of supRef by checking the satisfiability of subRef && !supRef. Creates a parser
     * for our SMT-ready refinement language and discharges the verification to Z3
     * 
     * @param subRef
     * @param supRef
     * @param context
     * 
     * @return the result of the verification, containing a counterexample if the verification fails
     */
    public SMTResult verifySubtype(Predicate subRef, Predicate supRef, Context context) throws Exception {
        Predicate toVerify = Predicate.createConjunction(subRef, supRef.negate());
        try {
            Expression exp = toVerify.getExpression();
            try (TranslatorToZ3 tz3 = new TranslatorToZ3(context)) {
                ExpressionToZ3Visitor visitor = new ExpressionToZ3Visitor(tz3);
                Expr<?> e = exp.accept(visitor);
                Solver solver = tz3.makeSolverForExpression(e);
                Status result = solver.check();

                // subRef is not a subtype of supRef
                if (result.equals(Status.SATISFIABLE)) {
                    Model model = solver.getModel();
                    Counterexample counterexample = tz3.getCounterexample(model);
                    return SMTResult.error(counterexample);
                }
            }
        } catch (SyntaxException e) {
            System.out.println("Could not parse: " + toVerify);
            e.printStackTrace();
        } catch (Z3Exception e) {
            throw new Z3Exception(e.getLocalizedMessage());
        }
        return SMTResult.ok();
    }
}
