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

    @StateRefinement(from = "open(this)")
    public void use() {}
}

class SamePackageHatch extends Hatch {

    static void useClosed() {
        SamePackageHatch hatch = new SamePackageHatch();
        hatch.use(); // Expect: State Refinement Error
    }
}
