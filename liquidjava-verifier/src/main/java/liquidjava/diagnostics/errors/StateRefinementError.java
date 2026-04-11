package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.rj_language.ast.Expression;
import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that a state refinement transition was violated
 * 
 * @see LJError
 */
public class StateRefinementError extends LJError {

    private final String expected;
    private final String found;

    public StateRefinementError(SourcePosition position, Expression expected, Expression found,
            TranslationTable translationTable, String customMessage) {
        super("State Refinement Error",
                String.format("Expected state %s but found %s", expected.toDisplayString(), found.toDisplayString()),
                position, translationTable, customMessage);
        this.expected = expected.toDisplayString();
        this.found = found.toDisplayString();
    }

    public String getExpected() {
        return expected;
    }

    public String getFound() {
        return found;
    }
}
