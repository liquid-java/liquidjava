package testSuite.classes.state_hints_inaccessible_inherited_error;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"closed", "open"})
public class Hatch {

    @StateRefinement(to = "closed(this)")
    public Hatch() {}

    @StateRefinement(from = "closed(this)", to = "open(this)")
    private void open() {}

    @StateRefinement(from = "closed(this)", to = "open(this)")
    void openInPackage() {}

    @StateRefinement(from = "closed(this)", to = "open(this)")
    protected void openInSubclass() {}

    @StateRefinement(from = "closed(this)", to = "open(this)")
    public void openPublicly() {}

    @StateRefinement(from = "open(this)")
    public void use() {}
}

// only openPublicly() can be called on a subclass by every caller
class SamePackageHatch extends Hatch {

    static void useClosed() {
        SamePackageHatch hatch = new SamePackageHatch();
        hatch.use(); // Expect: State Refinement Error
    }
}
