package liquidjava.diagnostics.errors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.LiteralString;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.ast.formatter.VariableFormatter;
import liquidjava.rj_language.opt.VCSimplificationResult;
import liquidjava.rj_language.parsing.RefinementsParser;
import liquidjava.smt.Counterexample;
import liquidjava.utils.Pair;
import spoon.reflect.cu.SourcePosition;

import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;

/**
 * Error indicating that a refinement constraint either was violated or cannot be proven
 * 
 * @see LJError
 */
public class RefinementError extends LJError {

    private final Predicate expected;
    private final Predicate finalExpected;
    private final Predicate expectedWithWitness;
    private final VCSimplificationResult found;
    private final Counterexample counterexample;
    private final SourcePosition declarationPosition;

    public RefinementError(SourcePosition position, SourcePosition declarationPosition, Predicate expected,
            VCSimplificationResult found, TranslationTable translationTable, Counterexample counterexample,
            String customMessage) {
        this(position, declarationPosition, expected, null, found, translationTable, counterexample, customMessage);
    }

    public RefinementError(SourcePosition position, SourcePosition declarationPosition, Predicate expected,
            Predicate finalExpected, VCSimplificationResult found, TranslationTable translationTable,
            Counterexample counterexample, String customMessage) {
        super("Refinement Error",
                String.format("%s is not a subtype of %s",
                        found.getImplication().toPredicate().getExpression().toDisplayString(),
                        expected.getExpression().toDisplayString()),
                position, translationTable, customMessage);
        this.expected = expected;
        this.finalExpected = finalExpected;
        this.found = found;
        this.counterexample = filterCounterexample(counterexample);
        this.expectedWithWitness = substituteWitness(finalExpected, this.counterexample);
        this.declarationPosition = declarationPosition;
        if (!this.counterexample.isEmpty()) {
            String counterexampleString = this.counterexample.assignments().stream()
                    .map(a -> VariableFormatter.format(a.first()) + " == " + a.second())
                    .collect(Collectors.joining(" && "));
            StringBuilder detail = new StringBuilder();
            if (finalExpected != null) {
                detail.append("Final expected: ").append(finalExpected.getExpression().toDisplayString()).append("\n");
            }
            detail.append("Counterexample: ").append(counterexampleString);
            if (expectedWithWitness != null) {
                detail.append("\nWith witness: ").append(expectedWithWitness.getExpression().toDisplayString());
                List<String> remainingVariables = new ArrayList<>();
                expectedWithWitness.getExpression().getVariableNames(remainingVariables);
                if (remainingVariables.isEmpty())
                    detail.append(" ✗");
            }
            setCounterexampleStr(detail.toString());
        }
        if (isTrue(found.getImplication().toPredicate().getExpression()))
            setHint("Not enough information to prove the expected refinement. Add a refinement or condition to constrain it.");
    }

    @Override
    public SourcePosition getDeclarationPosition() {
        return declarationPosition;
    }

    public Counterexample getCounterexample() {
        return counterexample;
    }

    public Predicate getExpected() {
        return expected;
    }

    public Predicate getFinalExpected() {
        return finalExpected;
    }

    public Predicate getExpectedWithWitness() {
        return expectedWithWitness;
    }

    public VCSimplificationResult getFound() {
        return found;
    }

    private static Predicate substituteWitness(Predicate finalExpected, Counterexample counterexample) {
        if (finalExpected == null || counterexample.isEmpty())
            return null;
        Expression expression = finalExpected.getExpression().clone();
        boolean substituted = false;
        for (Pair<String, String> assignment : counterexample.assignments()) {
            List<String> variableNames = new ArrayList<>();
            expression.getVariableNames(variableNames);
            if (!variableNames.contains(assignment.first()))
                continue;
            try {
                Expression value = RefinementsParser.createAST(assignment.second(), "");
                if (!isLiteralValue(value))
                    continue;
                expression = expression.substitute(new Var(assignment.first()), value);
                substituted = true;
            } catch (SyntaxError ignored) {
                // Some SMT values cannot be represented in the refinement language.
            }
        }
        return substituted ? new Predicate(expression) : null;
    }

    private static boolean isLiteralValue(Expression value) {
        if (value.isLiteral() || value instanceof LiteralString)
            return true;
        return value instanceof UnaryExpression unary && ("-".equals(unary.getOp()) || "+".equals(unary.getOp()))
                && isLiteralValue(unary.getExpression());
    }

    // Filters counterexample assignments only in found VC and sorts them in the order of its binders
    private Counterexample filterCounterexample(Counterexample counterexample) {
        if (counterexample == null)
            return new Counterexample(List.of());

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
