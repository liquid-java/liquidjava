package liquidjava.diagnostics.errors;

import static liquidjava.rj_language.opt.VCSimplificationUtils.factsOf;
import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;
import static liquidjava.rj_language.opt.VCSimplificationUtils.linkedToExpected;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.hints.Hint;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.opt.VCSimplificationResult;
import liquidjava.smt.Counterexample;
import liquidjava.utils.GeneratedNames;
import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that a state refinement transition was violated
 * 
 * @see LJError
 */
public class StateRefinementError extends LJError {

    private final Predicate expected;
    private final VCSimplificationResult found;
    private final SourcePosition declarationPosition;
    private final StateErrorContext context;
    private final List<String> declarationNotes;

    /**
     * @param display
     *            rewrites expected and found as the error shows them, here and to the IDE, after the facts to show are
     *            chosen on them as given
     * @param declarationNotes
     *            lines that explain the declaration, printed under its excerpt
     */
    public StateRefinementError(SourcePosition position, SourcePosition declarationPosition, Predicate expected,
            VCSimplificationResult found, TranslationTable translationTable, String customMessage, List<Hint> hints,
            ErrorCategory category, StateErrorContext context, UnaryOperator<Expression> display,
            List<String> declarationNotes) {
        super("State Refinement Error", ErrorMessage.of(expected.getExpression(), found, translationTable,
                StateRefinementError::appliesGhost, "found %s but expected %s", display), position, translationTable,
                customMessage);
        this.declarationPosition = declarationPosition;
        this.expected = new Predicate(display.apply(expected.getExpression()));
        this.found = found.map(display);
        this.context = context;
        this.declarationNotes = declarationNotes;
        hints.forEach(this::addHint);
        setCategory(category);
        if (hints.isEmpty() && isTrue(found.getImplication().toPredicate().getExpression()))
            addHint(Hint.general(
                    "No state information is known for this object. Make sure its state is initialized and available at this point."));
    }

    /**
     * Shows the values the counterexample gives the variables of the found the message shows, but those found already
     * states. A ghost or state value is never shown: a state is an unconstrained number, and the message already says
     * which state each object is in
     */
    public void showCounterexample(Counterexample counterexample) {
        Expression foundExpression = found.getImplication().toPredicate().getExpression();
        Expression shown = linkedToExpected(foundExpression, expected.getExpression());
        if (counterexample == null || !appliesGhost(shown))
            return;
        List<String> variables = new ArrayList<>();
        shown.getVariableNames(variables);
        List<Expression> facts = factsOf(foundExpression);
        String values = counterexample.assignments().stream()
                .filter(a -> variables.contains(a.first()) && !GeneratedNames.isGenerated(a.first()))
                .filter(a -> !RefinementError.isKnown(a, facts) && !a.second().contains("!val!"))
                .map(a -> getDisplayNames().get(a.first()) + " == " + a.second()).collect(Collectors.joining(" && "));
        if (!values.isEmpty())
            setCounterexampleStr("Counterexample: " + values);
    }

    // Only the facts of found about the objects of expected, and only when one of them tells a state: an alias or a
    // guard on another value says nothing about the state expected needs
    private static boolean appliesGhost(Expression expression) {
        return expression instanceof FunctionInvocation
                || expression.getChildren().stream().anyMatch(StateRefinementError::appliesGhost);
    }

    @Override
    public List<String> getDeclarationNotes() {
        return declarationNotes;
    }

    @Override
    public SourcePosition getDeclarationPosition() {
        return declarationPosition;
    }

    public Predicate getExpected() {
        return expected;
    }

    public VCImplication getFound() {
        return found.getImplication();
    }

    public VCSimplificationResult getFoundSimplification() {
        return found;
    }

    /** What is known about the receiver's states, or null if they could not be computed */
    public StateErrorContext getContext() {
        return context;
    }
}
