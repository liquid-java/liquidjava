package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that a refinement constraint either was violated or cannot be proven
 * 
 * @see LJError
 */
public class RefinementError extends LJError {

    private final ValDerivationNode expected;
    private final ValDerivationNode found;

    public RefinementError(SourcePosition position, ValDerivationNode expected, ValDerivationNode found,
            TranslationTable translationTable, String customMessage) {
        super("Refinement Error", String.format("%s is not a subtype of %s", found.getValue(), expected.getValue()),
                position, translationTable, customMessage);
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