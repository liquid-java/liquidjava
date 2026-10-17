package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"unbound", "bound"})
class CorrectRefinedArgListener {
    @StateRefinement(to = "unbound(this)")
    public CorrectRefinedArgListener() {}

    @StateRefinement(to = "bound(this)")
    public CorrectRefinedArgListener(@Refinement("0 <= _ && _ <= 65535") int port) {}

    @StateRefinement(from = "bound(this)")
    public void accept() {}
}

public class CorrectConstructorReceiverRefinedArgument {
    static void chained() {
        new CorrectRefinedArgListener(8080).accept();
    }
}
