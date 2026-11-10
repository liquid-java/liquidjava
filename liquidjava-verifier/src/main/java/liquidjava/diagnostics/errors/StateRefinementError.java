package liquidjava.diagnostics.errors;

import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;

import java.util.List;
import java.util.function.UnaryOperator;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.hints.Hint;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.opt.VCSimplificationResult;
import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that a state refinement transition was violated
 * 
 * @see LJError
 */
public class StateRefinementError extends LJError {

    private final Predicate expected;
    private final VCSimplificationResult found;
    private final List<SourcePosition> declarationPositions;
    private final StateErrorContext context;
    private final List<String> declarationNotes;

    /**
     * @param declarationPositions
     *            where each entry state that makes up expected is declared
     * @param display
     *            rewrites expected and found as the error shows them, here and to the IDE, after the facts to show are
     *            chosen on them as given
     * @param declarationNotes
     *            lines that explain the declaration, printed under its excerpt
     */
    public StateRefinementError(SourcePosition position, List<SourcePosition> declarationPositions, Predicate expected,
            VCSimplificationResult found, TranslationTable translationTable, String customMessage, List<Hint> hints,
            ErrorCategory category, StateErrorContext context, UnaryOperator<Expression> display,
            List<String> declarationNotes) {
        super("State Refinement Error", ErrorMessage.of(expected.getExpression(), found, translationTable,
                StateRefinementError::appliesGhost, "found %s but expected %s", display), position, translationTable,
                customMessage);
        this.declarationPositions = declarationPositions;
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
        return declarationPositions.isEmpty() ? null : declarationPositions.get(0);
    }

    @Override
    public List<SourcePosition> getDeclarationPositions() {
        return declarationPositions;
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
