package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleAliasedArgument {

    static void connect(@Refinement("_ >= 0") int timeout) {}

    static void probe(int port, int configured) {
        if (port < 0 || port > 65535)
            return;
        int timeout = configured;
        connect(timeout); // Expect: Refinement Error
    }
}
