package liquidjava.diagnostics.errors;

import spoon.reflect.cu.SourcePosition;

/**
 * Error indicating that a constructor contains a state refinement with a 'from' state, which is not allowed
 * 
 * @see LJError
 */
public class IllegalConstructorTransitionError extends LJError {

    public IllegalConstructorTransitionError(SourcePosition position) {
        super("Illegal Constructor Transition Error",
                "Found constructor with 'from' state: constructors should only have a 'to' state", position, null);
    }
}
