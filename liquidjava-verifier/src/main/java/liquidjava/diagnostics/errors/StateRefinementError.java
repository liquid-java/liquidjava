package liquidjava.diagnostics.errors;

import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;

import java.util.List;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.hints.Hint;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
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
    private final SourcePosition declarationPosition;

    public StateRefinementError(SourcePosition position, SourcePosition declarationPosition, Predicate expected,
            VCSimplificationResult found, TranslationTable translationTable, String customMessage, List<Hint> hints,
            ErrorCategory category) {
        super("State Refinement Error",
                String.format("found %s but expected %s",
                        found.getImplication().toPredicate().getExpression().toDisplayString(),
                        expected.getExpression().toDisplayString()),
                position, translationTable, customMessage);
        this.declarationPosition = declarationPosition;
        this.expected = expected;
        this.found = found;
        hints.forEach(this::addHint);
        setCategory(category);
        if (hints.isEmpty() && isTrue(found.getImplication().toPredicate().getExpression()))
            addHint(Hint.general(
                    "No state information is known for this object. Make sure its state is initialized and available at this point."));
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
}
