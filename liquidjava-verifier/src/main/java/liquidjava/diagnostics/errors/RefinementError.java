package liquidjava.diagnostics.errors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import liquidjava.diagnostics.AliasExpansion;
import liquidjava.diagnostics.TranslationTable;
import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.hints.Hint;
import liquidjava.processor.context.AliasWrapper;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.opt.VCSimplificationResult;
import liquidjava.smt.Counterexample;
import liquidjava.utils.GeneratedNames;
import liquidjava.utils.Pair;
import liquidjava.utils.Utils;
import spoon.reflect.cu.SourcePosition;

import static liquidjava.rj_language.opt.VCSimplificationUtils.factsOf;
import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;
import static liquidjava.rj_language.opt.VCSimplificationUtils.linkedTerms;
import static liquidjava.rj_language.opt.VCSimplificationUtils.linkedToExpected;
import static liquidjava.rj_language.opt.VCSimplificationUtils.termName;
import static liquidjava.rj_language.opt.VCSimplificationUtils.termsOf;

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
    private final List<AliasExpansion> foundAliases;
    private final List<AliasExpansion> expectedAliases;
    private final List<String> aliasDeclarations;

    public RefinementError(SourcePosition position, SourcePosition declarationPosition, Predicate expected,
            VCSimplificationResult found, TranslationTable translationTable, Counterexample counterexample,
            String customMessage, List<AliasWrapper> aliases) {
        super("Refinement Error",
                ErrorMessage.of(expected.getExpression(), found, translationTable, shown -> !isTrue(shown),
                        "%s is not a subtype of %s", UnaryOperator.identity()),
                position, translationTable, customMessage);
        this.expected = expected;
        this.found = found;
        Expression foundExpression = found.getImplication().toPredicate().getExpression();
        this.foundAliases = AliasExpansion.of(foundExpression, aliases, getDisplayNames());
        this.expectedAliases = AliasExpansion.of(expected.getExpression(), aliases, getDisplayNames());
        this.aliasDeclarations = aliasDeclarations(linkedToExpected(foundExpression, expected.getExpression()),
                expected.getExpression(), aliases);
        this.counterexample = filterCounterexample(counterexample);
        this.declarationPosition = declarationPosition;
        if (!this.counterexample.isEmpty()) {
            String counterexampleString = this.counterexample.assignments().stream()
                    .map(a -> getDisplayNames().get(a.first()) + " == " + a.second())
                    .collect(Collectors.joining(" && "));
            setCounterexampleStr("Counterexample: " + counterexampleString);
        }
        setCategory(categoryOf(expected, found));
        if (isTrue(found.getImplication().toPredicate().getExpression()))
            addHint(Hint.general(
                    "Not enough information to prove the expected refinement. Add a refinement or condition to constrain it."));
    }

    // Each alias the message uses once, with those their definitions use, as declared
    private List<String> aliasDeclarations(Expression shownFound, Expression expected, List<AliasWrapper> aliases) {
        Map<String, String> declarations = new LinkedHashMap<>();
        for (Expression shown : List.of(shownFound, expected))
            for (AliasExpansion expansion : AliasExpansion.of(shown, aliases, getDisplayNames()))
                declarations.putIfAbsent(expansion.alias(), expansion.declaration());
        return new ArrayList<>(declarations.values());
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

    /** The alias invocations in found and what they stand for */
    public List<AliasExpansion> getFoundAliases() {
        return foundAliases;
    }

    /** The alias invocations in expected and what they stand for */
    public List<AliasExpansion> getExpectedAliases() {
        return expectedAliases;
    }

    /** A where line declaring each alias the message uses, then the other notes */
    @Override
    public List<String> getMessageNotes() {
        List<String> notes = new ArrayList<>(
                aliasDeclarations.stream().map(declaration -> "where " + declaration).toList());
        notes.addAll(super.getMessageNotes());
        return notes;
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

    // Whether a fact of found is term == value, which the counterexample would only repeat
    private static boolean isKnown(Pair<String, String> assignment, List<Expression> facts) {
        return facts.stream()
                .anyMatch(fact -> fact instanceof BinaryExpression binary && "==".equals(binary.getOperator())
                        && (states(binary.getFirstOperand(), binary.getSecondOperand(), assignment)
                                || states(binary.getSecondOperand(), binary.getFirstOperand(), assignment)));
    }

    // An enum constant is written Format.JPG in found, either as such or as a variable, and JPG in the model; a static
    // final constant keeps its name in found and is its value in the model
    private static boolean states(Expression term, Expression value, Pair<String, String> assignment) {
        if (value instanceof Enum constant && constant.getResolvedLiteral() != null)
            value = constant.getResolvedLiteral();
        String valueName = value instanceof Enum constant ? constant.getConstName()
                : value instanceof Var var ? Utils.getSimpleName(var.getName()) : value.toString();
        return assignment.first().equals(termName(term)) && assignment.second().equals(valueName);
    }

    // Binders of the VC chain before simplification, which keeps the variables in the order they were introduced
    private List<String> originalBinders() {
        return getFound().getOriginal().getBinders();
    }

    // 0 for terms of expected found says nothing about, whose values alone break it, 1 for the other terms of
    // expected, 2 for the terms linked to them and 3 for generated names among those
    private static int relevance(String term, List<String> expectedTerms, List<String> foundTerms) {
        if (expectedTerms.contains(term))
            return foundTerms.contains(term) ? 1 : 0;
        return GeneratedNames.isGenerated(term) ? 3 : 2;
    }
}
