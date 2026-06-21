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

    public StateRefinementError(SourcePosition position, Predicate expected, VCSimplificationResult found,
            TranslationTable translationTable, String customMessage) {
        super("State Refinement Error",
                String.format("Expected state %s but found %s", expected.getExpression().toDisplayString(),
                        found.getImplication().toPredicate().getExpression().toDisplayString()),
                position, translationTable, customMessage);
        this.expected = expected;
        this.found = found;
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
