package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleArrayIndex {

    static void get(int[] a, @Refinement("_ >= 0 && _ < length(a)") int i) {}

    static void probe(int port, int k) {
        if (port < 0)
            return;
        int[] a = new int[4];
        get(a, k); // Expect: Refinement Error
    }
}
