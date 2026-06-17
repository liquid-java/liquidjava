package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.processor.VCImplication;
import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that a state refinement transition was violated
 * 
 * @see LJError
 */
public class StateRefinementError extends LJError {

    private final VCImplication expected;
    private final VCImplication found;

    public StateRefinementError(SourcePosition position, VCImplication expected, VCImplication found,
            TranslationTable translationTable, String customMessage) {
        super("State Refinement Error",
                String.format("Expected state %s but found %s",
                        expected.toPredicate().getExpression().toDisplayString(),
                        found.toPredicate().getExpression().toDisplayString()),
                position, translationTable, customMessage);
        this.expected = expected;
        this.found = found;
    }

    public VCImplication getExpected() {
        return expected;
    }

    public VCImplication getFound() {
        return found;
    }
}
