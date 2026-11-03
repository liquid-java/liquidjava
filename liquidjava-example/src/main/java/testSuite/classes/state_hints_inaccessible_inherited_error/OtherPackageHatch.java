package testSuite.classes.state_hints_inaccessible_inherited_error.elsewhere;

import testSuite.classes.state_hints_inaccessible_inherited_error.Hatch;

public class OtherPackageHatch extends Hatch {

    static void useClosed() {
        OtherPackageHatch hatch = new OtherPackageHatch();
        hatch.use(); // Expect: State Refinement Error
    }
}
