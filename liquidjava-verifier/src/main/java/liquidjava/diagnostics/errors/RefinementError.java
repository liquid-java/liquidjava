package liquidjava.diagnostics.errors;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.formatter.VariableFormatter;
import liquidjava.rj_language.opt.VCSimplificationResult;
import liquidjava.smt.Counterexample;
import liquidjava.utils.Pair;
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
    private final SourcePosition declarationPosition;

    public RefinementError(SourcePosition position, SourcePosition declarationPosition, Predicate expected,
            VCSimplificationResult found, TranslationTable translationTable, Counterexample counterexample,
            String customMessage) {
        super("Refinement Error",
                String.format("%s is not a subtype of %s",
                        found.getImplication().toPredicate().getExpression().toDisplayString(),
                        expected.getExpression().toDisplayString()),
                position, translationTable, customMessage);
        this.expected = expected;
        this.found = found;
        this.counterexample = filterCounterexample(counterexample);
        this.declarationPosition = declarationPosition;
    }

    @Override
    public SourcePosition getDeclarationPosition() {
        return declarationPosition;
    }

    @Override
    public String getHint() {
        if (counterexample.isEmpty())
            return null;

        String counterexampleString = counterexample.assignments().stream()
                .map(a -> VariableFormatter.format(a.first()) + " == " + a.second())
                .collect(Collectors.joining(" && "));
        return "Counterexample: " + counterexampleString;
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

    // Filters counterexample assignments only in found VC and sorts them in the order of its binders
    private Counterexample filterCounterexample(Counterexample counterexample) {
        List<String> binderNames = getFound().getBinders();
        Set<String> knownAssignments = getFound().getImplication().toPredicate().getExpression().getConjuncts().stream()
                .map(Expression::toString).collect(Collectors.toSet());
        List<Pair<String, String>> assignments = counterexample.assignments().stream()
                .filter(a -> binderNames.contains(a.first()))
                .filter(a -> !knownAssignments.contains(a.first() + " == " + a.second()))
                .sorted((a, b) -> Integer.compare(binderNames.indexOf(a.first()), binderNames.indexOf(b.first())))
                .toList();

        return new Counterexample(assignments);
    }
}
