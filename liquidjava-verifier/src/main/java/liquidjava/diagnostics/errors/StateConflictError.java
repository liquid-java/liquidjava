package liquidjava.diagnostics.errors;

import liquidjava.diagnostics.TranslationTable;
import liquidjava.rj_language.ast.Expression;
import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that two disjoint states were found in a state refinement
 * 
 * @see LJError
 */
public class StateConflictError extends LJError {

    private final String state;;

    public StateConflictError(SourcePosition position, Expression state, TranslationTable translationTable) {
        super("State Conflict Error",
                "Found multiple disjoint states in state transition: state transition can only go to one state of each state set",
                position, translationTable);
        this.state = state.toString();
    }

    public String getState() {
        return state;
    }
}
