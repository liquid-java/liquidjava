package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.rj_language.ast.Expression;
import liquidjava.utils.VariableNameFormatter;
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
                String.format("Expected state %s but found %s", VariableNameFormatter.formatText(expected.toString()),
                        VariableNameFormatter.formatText(found.toString())),
                position, translationTable, customMessage);
        this.expected = VariableNameFormatter.formatText(expected.toString());
        this.found = VariableNameFormatter.formatText(found.toString());
    }

    public String getExpected() {
        return expected;
    }

    public String getFound() {
        return found;
    }
}
