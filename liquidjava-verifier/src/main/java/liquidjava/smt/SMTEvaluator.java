package liquidjava.smt;

import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;

import java.util.Optional;

import liquidjava.diagnostics.DebugLog;
import liquidjava.diagnostics.errors.SMTUnknownError;
import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.utils.CacheMode;

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
        return verifySubtype(subRef, supRef, context, false);
    }

    public SMTResult verifySubtype(Predicate subRef, Predicate supRef, Context context, boolean silent)
            throws Exception {
        return verifySubtype(subRef, supRef, context, silent, true);
    }

    /**
     * @param needsCounterexample
     *            whether the caller uses the counterexample of a failing check, which a result kept from an earlier run
     *            only has when the variables have the same names
     */
    public SMTResult verifySubtype(Predicate subRef, Predicate supRef, Context context, boolean silent,
            boolean needsCounterexample) throws Exception {
        if (!silent) {
            DebugLog.smtStart(subRef, supRef);
        }
        Predicate toVerify = Predicate.createConjunction(subRef, supRef.negate());
        try {
            return check(toVerify.getExpression(), context, silent, needsCounterexample);
        } catch (Z3Exception e) {
            throw new Z3Exception(e.getLocalizedMessage());
        }
    }

    public boolean isUnsatisfiable(Predicate predicate, Context context) throws Exception {
        try {
            return check(predicate.getExpression(), context, true, false).isOk();
        } catch (SMTUnknownError e) {
            return false;
        } catch (Z3Exception e) {
            throw new Z3Exception(e.getLocalizedMessage());
        }
    }

    /**
     * Whether {@code exp} is unsatisfiable, with a counterexample when it is not, reusing the result of an earlier run
     */
    private SMTResult check(Expression exp, Context context, boolean silent, boolean needsCounterexample)
            throws Exception {
        CacheMode mode = CacheMode.current();
        if (mode == CacheMode.OFF)
            return solve(exp, context, silent);
        QueryCache.Key key = QueryCache.key(exp, context);
        Optional<SMTResult> cached = QueryCache.get(key, needsCounterexample);
        if (cached.isPresent() && mode == CacheMode.ON) {
            if (!silent) {
                if (cached.get().isOk())
                    DebugLog.smtUnsat();
                else
                    DebugLog.smtSat(cached.get().getCounterexample());
            }
            return cached.get();
        }
        SMTResult result = solve(exp, context, silent);
        if (cached.isPresent())
            QueryCache.checkSame(key, cached.get(), result);
        else
            QueryCache.put(key, result);
        return result;
    }

    private SMTResult solve(Expression exp, Context context, boolean silent) throws Exception {
        try (TranslatorToZ3 tz3 = new TranslatorToZ3(context)) {
            ExpressionToZ3Visitor visitor = new ExpressionToZ3Visitor(tz3);
            Expr<?> e = exp.accept(visitor);
            Solver solver = tz3.makeSimpleSolverForExpression(e);
            Status result = solver.check();
            if (result.equals(Status.UNKNOWN)) {
                solver = tz3.makeSolverForExpression(e);
                result = solver.check();
            }

            // subRef is not a subtype of supRef
            if (result.equals(Status.SATISFIABLE)) {
                Model model = solver.getModel();
                Counterexample counterexample = tz3.getCounterexample(model);
                if (!silent) {
                    DebugLog.smtSat(counterexample);
                }
                return SMTResult.error(counterexample);
            }
            if (result.equals(Status.UNKNOWN)) {
                SMTUnknownError error = new SMTUnknownError(solver.getReasonUnknown());
                if (!silent) {
                    DebugLog.smtError(error.getMessage());
                }
                throw error;
            }
            if (!silent) {
                DebugLog.smtUnsat();
            }
            return SMTResult.ok();
        }
    }
}
