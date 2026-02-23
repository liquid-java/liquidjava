package liquidjava.diagnostics.errors;

import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that a refinement is invalid (e.g., not a boolean expression)
 * 
 * @see LJError
 */
public class InvalidRefinementError extends LJError {

    private final String refinement;

    public InvalidRefinementError(SourcePosition position, String message, String refinement) {
        super("Invalid Refinement Error", message, position, null);
        this.refinement = refinement;
    }

    public String getRefinement() {
        return refinement;
    }
}
