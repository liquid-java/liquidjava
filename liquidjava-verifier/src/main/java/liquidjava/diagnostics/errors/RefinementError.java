package liquidjava.diagnostics.errors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.formatter.VariableFormatter;
import liquidjava.rj_language.opt.VCSimplificationResult;
import liquidjava.smt.Counterexample;
import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that a refinement constraint either was violated or cannot be proven
 * 
 * @see LJError
 */
public class RefinementError extends LJError {

    private final Predicate expected;
    private final VCSimplificationResult found;
    private final Counterexample counterexample;

    public RefinementError(SourcePosition position, Predicate expected, VCSimplificationResult found,
            TranslationTable translationTable, Counterexample counterexample, String customMessage) {
        super("Refinement Error",
                String.format("%s is not a subtype of %s",
                        found.getImplication().toPredicate().getExpression().toDisplayString(),
                        expected.getExpression().toDisplayString()),
                position, translationTable, customMessage);
        this.expected = expected;
        this.found = found;
        this.counterexample = counterexample;
    }

    @Override
    public String getDetails() {
        String counterexampleString = getCounterExampleString();
        if (counterexampleString == null)
            return "";
        return "Counterexample: " + counterexampleString;
    }

    public String getCounterExampleString() {
        if (counterexample == null || counterexample.assignments().isEmpty())
            return null;

        List<String> foundVarNames = new ArrayList<>();
        Expression foundExpression = getFound().getImplication().toPredicate().getExpression();
        Expression expectedExpression = expected.getExpression();
        foundExpression.getVariableNames(foundVarNames);
        // also keep resolved static-final constants (e.g. Integer.MAX_VALUE) referenced by either side of the
        // subtyping check, so the counterexample maps the symbolic name back to its compile-time value
        foundExpression.getResolvedConstantNames(foundVarNames);
        expectedExpression.getResolvedConstantNames(foundVarNames);
        List<String> foundAssignments = foundExpression.getConjuncts().stream().map(Expression::toString).toList();
        String counterexampleString = counterexample.assignments().stream()
                // only include variables that appear in the found value and are not already fixed there
                .filter(a -> foundVarNames.contains(a.first())
                        && !foundAssignments.contains(a.first() + " == " + a.second()))
                // format as "var == value"
                .map(a -> VariableFormatter.format(a.first()) + " == " + a.second())
                // join with "&&"
                .collect(Collectors.joining(" && "));

        if (counterexampleString.isEmpty())
            return null;

        return counterexampleString;
    }

    public Counterexample getCounterexample() {
        return counterexample;
    }

    public Predicate getExpected() {
        return expected;
    }

    public VCSimplificationResult getFound() {
        return found;
    }
}
