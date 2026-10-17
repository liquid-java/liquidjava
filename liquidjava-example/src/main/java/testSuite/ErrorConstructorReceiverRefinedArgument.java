package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"unbound", "bound"})
class ErrorRefinedArgListener {
    @StateRefinement(to = "unbound(this)")
    public ErrorRefinedArgListener() {}

    @StateRefinement(to = "bound(this)")
    public ErrorRefinedArgListener(@Refinement("0 <= _ && _ <= 65535") int port) {}

    @StateRefinement(from = "unbound(this)", to = "bound(this)")
    public void bind() {}
}

public class ErrorConstructorReceiverRefinedArgument {
    static void chained() {
        new ErrorRefinedArgListener(8080).bind(); // Expect: State Refinement Error
    }
}
