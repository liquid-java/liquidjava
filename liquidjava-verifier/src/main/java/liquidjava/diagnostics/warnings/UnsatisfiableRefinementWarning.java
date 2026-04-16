package liquidjava.diagnostics.warnings;

import spoon.reflect.cu.SourcePosition;

/**
 * Warning indicating that a refinement predicate is unsatisfiable
 * 
 * @see LJWarning
 */
public class UnsatisfiableRefinementWarning extends LJWarning {

    private final String refinement;

    public UnsatisfiableRefinementWarning(SourcePosition position, String refinement) {
        super("This refinement can never be true", position);
        this.refinement = refinement;
    }

    public String getRefinement() {
        return refinement;
    }
}
