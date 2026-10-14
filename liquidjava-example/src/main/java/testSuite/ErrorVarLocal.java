package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;

// The value of a var local is still checked against a refined parameter (#347)
@SuppressWarnings("unused")
@RefinementAlias("Port(int x) { 0 <= x && x <= 65535 }")
public class ErrorVarLocal {
    static void connect(@Refinement("Port(_)") int port) {
    }

    static void refinedArgument() {
        var port = 70000;
        connect(port); // Expect: Refinement Error
    }
}
