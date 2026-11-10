package liquidjava.diagnostics.errors;

import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.hints.Hint;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.opt.VCSimplificationResult;
import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that a state refinement transition was violated
 * 
 * @see LJError
 */
public class StateRefinementError extends LJError {

    /** A call with one argument that is a name, e.g. {@code closed(this)} */
    private static final Pattern ON_A_NAME = Pattern.compile("([A-Za-z_$][\\w$.]*)\\(\\s*([A-Za-z_$][\\w$]*)\\s*\\)");

    private final Predicate expected;
    private final VCSimplificationResult found;
    private final SourcePosition declarationPosition;
    private final StateErrorContext context;
    private final UnaryOperator<Expression> display;

    /**
     * @param display
     *            rewrites expected and found as the error shows them, here and to the IDE, after the facts to show are
     *            chosen on them as given
     */
    public StateRefinementError(SourcePosition position, SourcePosition declarationPosition, Predicate expected,
            VCSimplificationResult found, TranslationTable translationTable, String customMessage, List<Hint> hints,
            ErrorCategory category, StateErrorContext context, UnaryOperator<Expression> display) {
        super("State Refinement Error", ErrorMessage.of(expected.getExpression(), found, translationTable,
                StateRefinementError::appliesGhost, "found %s but expected %s", display), position, translationTable,
                customMessage);
        this.declarationPosition = declarationPosition;
        this.expected = new Predicate(display.apply(expected.getExpression()));
        this.found = found.map(display);
        this.context = context;
        this.display = display;
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

    /**
     * What each state the declaration writes as a shorthand means as the message shows it, e.g.
     * {@code closed(this) means doorState(this) == closed}: the declaration is shown as written
     */
    @Override
    public List<String> getDeclarationNotes(String declaration) {
        List<String> meanings = new ArrayList<>();
        Matcher call = ON_A_NAME.matcher(declaration);
        while (call.find()) {
            Expression state = new FunctionInvocation(call.group(1), List.of(new Var(call.group(2))));
            Expression shown = display.apply(state);
            String meaning = state.toDisplayString() + " means " + shown.toDisplayString();
            if (!shown.equals(state) && !meanings.contains(meaning))
                meanings.add(meaning);
        }
        return meanings.isEmpty() ? List.of() : List.of("where " + String.join(", ", meanings));
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
