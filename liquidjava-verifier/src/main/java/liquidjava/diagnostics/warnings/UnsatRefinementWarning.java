package liquidjava.diagnostics.warnings;

import spoon.reflect.cu.SourcePosition;

/**
 * Warning indicating that a refinement predicate is unsatisfiable
 * 
 * @see LJWarning
 */
public class UnsatRefinementWarning extends LJWarning {

    private final String refinement;

    public UnsatRefinementWarning(SourcePosition position, String message, String refinement) {
        super(message, position);
        this.refinement = refinement;
    }

    public String getRefinement() {
        return refinement;
    }
}
