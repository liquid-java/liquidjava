package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleArrayArgument {

    static void get(int[] a, @Refinement("_ >= 0 && _ < length(a)") int i) {}

    static void probe(int port, int[] a) {
        if (port < 0)
            return;
        get(a, 3); // Expect: Refinement Error
    }
}
