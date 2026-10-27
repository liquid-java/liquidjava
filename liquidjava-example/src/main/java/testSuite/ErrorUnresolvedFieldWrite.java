package testSuite;

import org.example.missing.Options;

import liquidjava.specification.Refinement;

// A violation after a write to a field of a class that cannot be resolved is still reported (#426)
@SuppressWarnings("unused")
public class ErrorUnresolvedFieldWrite {
    static void positive(@Refinement("_ > 0") int x) {
    }

    static void configure(Options opts) {
        opts.sampleSize = 1;
        positive(-1); // Expect: Refinement Error
    }
}
