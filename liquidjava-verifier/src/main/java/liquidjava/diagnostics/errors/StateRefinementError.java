package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that a state refinement transition was violated
 * 
 * @see LJError
 */
public class StateRefinementError extends LJError {

    private final ValDerivationNode expected;
    private final ValDerivationNode found;

    public StateRefinementError(SourcePosition position, ValDerivationNode expected, ValDerivationNode found,
            TranslationTable translationTable, String customMessage) {
        super("State Refinement Error", String.format("Expected state %s but found %s",
                expected.getValue().toDisplayString(), found.getValue().toDisplayString()), position, translationTable,
                customMessage);
        this.expected = expected;
        this.found = found;
    }

    public ValDerivationNode getExpected() {
        return expected;
    }

    public ValDerivationNode getFound() {
        return found;
    }
}
