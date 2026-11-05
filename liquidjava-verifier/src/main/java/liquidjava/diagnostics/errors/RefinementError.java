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
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.ast.formatter.VariableFormatter;
import liquidjava.rj_language.opt.VCSimplificationResult;
import liquidjava.smt.Counterexample;
import liquidjava.utils.GeneratedNames;
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

    private final Predicate expected;
    private final VCSimplificationResult found;
    private final Counterexample counterexample;
    private final SourcePosition declarationPosition;

    public RefinementError(SourcePosition position, SourcePosition declarationPosition, Predicate expected,
            VCSimplificationResult found, TranslationTable translationTable, Counterexample counterexample,
            String customMessage) {
        super("Refinement Error", message(expected, found), position, translationTable, customMessage);
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

    // Only the facts of found about the values expected constrains; path conditions about other values stay in the
    // derivation
    private static String message(Predicate expected, VCSimplificationResult found) {
        Expression shown = linkedToExpected(found.getImplication().toPredicate().getExpression(),
                expected.getExpression());
        if (isTrue(shown))
            return String.format("expected %s but could not prove it", expected.getExpression().toDisplayString());
        return String.format("%s is not a subtype of %s", shown.toDisplayString(),
                expected.getExpression().toDisplayString());
    }

    /**
     * The facts of found linked to the terms of expected, as a conjunction, or true when there are none; found as
     * written when every fact is linked
     */
    static Expression linkedToExpected(Expression found, Expression expected) {
        List<Expression> facts = factsOf(found);
        Set<String> linked = linkedTerms(facts, termsOf(expected));
        List<Expression> linkedFacts = facts.stream().filter(fact -> termsOf(fact).stream().anyMatch(linked::contains))
                .toList();
        if (linkedFacts.size() == facts.size())
            return found;
        return linkedFacts.stream().reduce((first, second) -> new BinaryExpression(first, "&&", second))
                .orElse(new LiteralBoolean(true));
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

    // Keeps the counterexample assignments to the terms of expected and of the found facts linked to them, except
    // values found already states, so there is none when found fixes every value expected depends on. The terms of
    // expected come first, those found says nothing about before the rest, then the linked terms in the order of the
    // VC binders, with generated names last
    private Counterexample filterCounterexample(Counterexample counterexample) {
        if (counterexample == null)
            return new Counterexample(List.of());

        Expression foundExpression = getFound().getImplication().toPredicate().getExpression();
        List<Expression> facts = factsOf(foundExpression);
        List<String> foundTerms = termsOf(foundExpression);
        List<String> expectedTerms = termsOf(getExpected().getExpression());
        Set<String> linkedTerms = expectedTerms.isEmpty() ? new HashSet<>(foundTerms)
                : linkedTerms(facts, expectedTerms);
        List<Pair<String, String>> assignments = counterexample.assignments().stream()
                .filter(a -> linkedTerms.contains(a.first())).filter(a -> !isKnown(a, facts)).toList();

        List<String> order = new ArrayList<>(originalBinders());
        order.addAll(foundTerms);
        order.addAll(expectedTerms);
        return new Counterexample(assignments.stream()
                .sorted(Comparator
                        .<Pair<String, String>> comparingInt(a -> relevance(a.first(), expectedTerms, foundTerms))
                        .thenComparingInt(a -> order.indexOf(a.first())))
                .toList());
    }

    // Conjuncts of found, also splitting negated disjunctions (!(a || b) is !a && !b) and dropping double negations, so
    // each fact links only the terms it relates
    private static List<Expression> factsOf(Expression expression) {
        List<Expression> facts = new ArrayList<>();
        if (expression instanceof BinaryExpression binary && "&&".equals(binary.getOperator())) {
            facts.addAll(factsOf(binary.getFirstOperand()));
            facts.addAll(factsOf(binary.getSecondOperand()));
        } else if (expression instanceof UnaryExpression not && "!".equals(not.getOp())
                && not.getExpression()instanceof BinaryExpression binary && "||".equals(binary.getOperator())) {
            facts.addAll(factsOf(new UnaryExpression("!", binary.getFirstOperand())));
            facts.addAll(factsOf(new UnaryExpression("!", binary.getSecondOperand())));
        } else if (expression instanceof UnaryExpression not && "!".equals(not.getOp())
                && not.getExpression()instanceof UnaryExpression inner && "!".equals(inner.getOp())) {
            facts.addAll(factsOf(inner.getExpression()));
        } else {
            facts.add(expression);
        }
        return facts;
    }

    // Terms of expected plus those of every fact sharing a term with them, directly or through other facts. The
    // remaining facts constrain other values only, so any values satisfying them make the same counterexample
    private static Set<String> linkedTerms(List<Expression> facts, List<String> expectedTerms) {
        Set<String> linked = new HashSet<>(expectedTerms);
        List<List<String>> unlinked = facts.stream().map(RefinementError::termsOf)
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

    // Whether a fact of found is term == value, which the counterexample would only repeat
    private static boolean isKnown(Pair<String, String> assignment, List<Expression> facts) {
        return facts.stream()
                .anyMatch(fact -> fact instanceof BinaryExpression binary && "==".equals(binary.getOperator())
                        && (states(binary.getFirstOperand(), binary.getSecondOperand(), assignment)
                                || states(binary.getSecondOperand(), binary.getFirstOperand(), assignment)));
    }

    // An enum constant is written Format.JPG in found, either as such or as a variable, and JPG in the model
    private static boolean states(Expression term, Expression value, Pair<String, String> assignment) {
        String valueName = value instanceof Enum constant ? constant.getConstName()
                : value instanceof Var var ? Utils.getSimpleName(var.getName()) : value.toString();
        return assignment.first().equals(termName(term)) && assignment.second().equals(valueName);
    }

    // Binders of the VC chain before simplification, which keeps the variables in the order they were introduced
    private List<String> originalBinders() {
        VCSimplificationResult original = getFound();
        while (original.getOrigin() != null)
            original = original.getOrigin();
        return original.getBinders();
    }

    // 0 for terms of expected found says nothing about, whose values alone break it, 1 for the other terms of
    // expected, 2 for the terms linked to them and 3 for generated names among those
    private static int relevance(String term, List<String> expectedTerms, List<String> foundTerms) {
        if (expectedTerms.contains(term))
            return foundTerms.contains(term) ? 1 : 0;
        return GeneratedNames.isGenerated(term) ? 3 : 2;
    }

    // Variables and ghost applications of an expression, named as in the SMT model
    private static List<String> termsOf(Expression expression) {
        List<String> terms = new ArrayList<>();
        expression.getVariableNames(terms);
        addGhostApplications(expression, terms);
        return terms;
    }

    private static void addGhostApplications(Expression expression, List<String> terms) {
        String term = expression instanceof FunctionInvocation ? termName(expression) : null;
        if (term != null && !terms.contains(term))
            terms.add(term);
        for (Expression child : expression.getChildren())
            addGhostApplications(child, terms);
    }

    // Name of a variable, or of a ghost application over variables, as in the SMT model; null for other expressions
    private static String termName(Expression expression) {
        if (expression instanceof Var var)
            return var.getName();
        if (expression instanceof FunctionInvocation invocation
                && invocation.getArgs().stream().allMatch(Var.class::isInstance)) {
            String args = invocation.getArgs().stream().map(Expression::toString).collect(Collectors.joining(", "));
            return Utils.getSimpleName(invocation.getName()) + "(" + args + ")";
        }
        return null;
    }
}
