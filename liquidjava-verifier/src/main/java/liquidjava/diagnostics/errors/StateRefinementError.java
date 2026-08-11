package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.TranslationTable;
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
            VCSimplificationResult found, TranslationTable translationTable, String customMessage) {
        super("State Refinement Error",
                String.format("found %s but expected %s",
                        found.getImplication().toPredicate().getExpression().toDisplayString(),
                        expected.getExpression().toDisplayString()),
                position, translationTable, customMessage);
        this.declarationPosition = declarationPosition;
        this.expected = expected;
        this.found = found;
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
