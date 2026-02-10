package liquidjava.smt;

import com.martiansoftware.jsap.SyntaxException;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;

import liquidjava.diagnostics.Counterexample;
import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;

public class SMTEvaluator {

    public void verifySubtype(Predicate subRef, Predicate supRef, Context c) throws Exception {
        // Creates a parser for our SMT-ready refinement language
        // Discharges the verification to z3
        Predicate toVerify = Predicate.createConjunction(subRef, supRef.negate());
        try {
            Expression exp = toVerify.getExpression();
            try (TranslatorToZ3 tz3 = new TranslatorToZ3(c)) {
                ExpressionToZ3Visitor visitor = new ExpressionToZ3Visitor(tz3);
                Expr<?> e = exp.accept(visitor);
                Solver solver = tz3.makeSolverForExpression(e);
                Status result = solver.check();
                if (result.equals(Status.SATISFIABLE)) {
                    Model model = solver.getModel();
                    Counterexample counterexample = tz3.getCounterexample(model);
                    throw new TypeCheckError(counterexample);
                }
            }
        } catch (SyntaxException e1) {
            System.out.println("Could not parse: " + toVerify);
            e1.printStackTrace();
        } catch (Z3Exception e) {
            throw new Z3Exception(e.getLocalizedMessage());
        }
    }
}
