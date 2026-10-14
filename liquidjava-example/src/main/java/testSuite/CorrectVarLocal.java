package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;

// A var local has the static type of its initialiser (#344, #347)
@SuppressWarnings("unused")
@RefinementAlias("Port(int x) { 0 <= x && x <= 65535 }")
public class CorrectVarLocal {
    static String name() {
        return "x";
    }

    static void connect(@Refinement("Port(_)") int port) {
    }

    static void nullCheck() {
        var n = name();
        if (n == null) {
            throw new IllegalArgumentException("no name");
        }
    }

    static void refinedArgument() {
        var port = 80;
        connect(port);
    }
}
