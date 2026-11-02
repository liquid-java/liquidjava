package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleNothingKnown {

    static void sleep(@Refinement("_ >= 0") int millis) {}

    static void probe(int millis) {
        sleep(millis); // Expect: Refinement Error
    }
}
