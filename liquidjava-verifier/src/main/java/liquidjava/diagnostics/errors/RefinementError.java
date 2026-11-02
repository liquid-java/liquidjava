package liquidjava.diagnostics.errors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.hints.Hint;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.ast.formatter.VariableFormatter;
import liquidjava.rj_language.opt.VCSimplificationResult;
import liquidjava.smt.Counterexample;
import liquidjava.utils.Pair;
import liquidjava.utils.Utils;
import spoon.reflect.cu.SourcePosition;

import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;

/**
 * Error indicating that a refinement constraint either was violated or cannot be proven
 * 
 * @see LJError
 */
public class RefinementError extends LJError {

    private static final String FRESH_PREFIX = "#fresh_";

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
        if (!this.counterexample.isEmpty()) {
            String counterexampleString = this.counterexample.assignments().stream()
                    .map(a -> VariableFormatter.format(a.first()) + " == " + a.second())
                    .collect(Collectors.joining(" && "));
            setCounterexampleStr("Counterexample: " + counterexampleString);
        }
        setCategory(categoryOf(expected, found));
        if (isTrue(found.getImplication().toPredicate().getExpression()))
            addHint(Hint.general(
                    "Not enough information to prove the expected refinement. Add a refinement or condition to constrain it."));
    }

    /** Unknown when what was found says nothing about the values the expected refinement constrains */
    private static ErrorCategory categoryOf(Predicate expected, VCSimplificationResult found) {
        List<String> foundVariables = found.getImplication().toPredicate().getVariableNames();
        boolean constrained = expected.getVariableNames().stream().anyMatch(foundVariables::contains);
        return constrained ? ErrorCategory.OUT_OF_DOMAIN : ErrorCategory.UNKNOWN;
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

    public VCSimplificationResult getFound() {
        return found;
    }

    // Keeps the counterexample assignments to the terms of expected and of the found conjuncts linked to them, except
    // values found already states, listing the terms only expected mentions first, generated names last, and the rest
    // in the order of the VC binders
    private Counterexample filterCounterexample(Counterexample counterexample) {
        if (counterexample == null)
            return new Counterexample(List.of());

        Expression foundExpression = getFound().getImplication().toPredicate().getExpression();
        List<Expression> conjuncts = foundExpression.getConjuncts();
        List<String> foundTerms = termsOf(foundExpression);
        List<String> expectedTerms = termsOf(getExpected().getExpression());
        Set<String> relevantTerms = expectedTerms.isEmpty() ? new HashSet<>(foundTerms)
                : linkedTerms(conjuncts, expectedTerms);
        List<String> order = new ArrayList<>(originalBinders());
        order.addAll(foundTerms);
        order.addAll(expectedTerms);
        Set<String> knownAssignments = conjuncts.stream().map(Expression::toString).collect(Collectors.toSet());
        List<Pair<String, String>> assignments = counterexample.assignments().stream()
                .filter(a -> relevantTerms.contains(a.first()))
                .filter(a -> !knownAssignments.contains(a.first() + " == " + a.second()))
                .sorted(Comparator.<Pair<String, String>> comparingInt(a -> relevance(a.first(), foundTerms))
                        .thenComparingInt(a -> order.indexOf(a.first())))
                .toList();

        return new Counterexample(assignments);
    }

    // Terms of expected plus those of every conjunct sharing a term with them, directly or through other conjuncts.
    // The remaining conjuncts constrain other values only, so any values satisfying them make the same counterexample
    private static Set<String> linkedTerms(List<Expression> conjuncts, List<String> expectedTerms) {
        Set<String> linked = new HashSet<>(expectedTerms);
        List<List<String>> unlinked = conjuncts.stream().map(RefinementError::termsOf)
                .collect(Collectors.toCollection(ArrayList::new));
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Iterator<List<String>> it = unlinked.iterator(); it.hasNext();) {
                List<String> terms = it.next();
                if (terms.stream().anyMatch(linked::contains)) {
                    linked.addAll(terms);
                    it.remove();
                    changed = true;
                }
            }
        }
        return linked;
    }

    // Binders of the VC chain before simplification, which keeps the variables in the order they were introduced
    private List<String> originalBinders() {
        VCSimplificationResult original = getFound();
        while (original.getOrigin() != null)
            original = original.getOrigin();
        return original.getBinders();
    }

    // 0 when found says nothing about the term, so its value alone breaks expected; 2 for generated names
    private static int relevance(String term, List<String> foundTerms) {
        if (!foundTerms.contains(term))
            return 0;
        return term.startsWith(FRESH_PREFIX) ? 2 : 1;
    }

    // Variables and ghost applications of an expression, named as in the SMT model
    private static List<String> termsOf(Expression expression) {
        List<String> terms = new ArrayList<>();
        expression.getVariableNames(terms);
        addGhostApplications(expression, terms);
        return terms;
    }

    private static void addGhostApplications(Expression expression, List<String> terms) {
        if (expression instanceof FunctionInvocation invocation
                && invocation.getArgs().stream().allMatch(Var.class::isInstance)) {
            String args = invocation.getArgs().stream().map(Expression::toString).collect(Collectors.joining(", "));
            String term = Utils.getSimpleName(invocation.getName()) + "(" + args + ")";
            if (!terms.contains(term))
                terms.add(term);
        }
        for (Expression child : expression.getChildren())
            addGhostApplications(child, terms);
    }
}
