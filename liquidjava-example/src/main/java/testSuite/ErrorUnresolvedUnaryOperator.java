package testSuite;

import org.example.missing.Settings;

import liquidjava.specification.Refinement;

// A violation under a negation of a call whose type cannot be resolved is still reported (#389)
@SuppressWarnings("unused")
public class ErrorUnresolvedUnaryOperator {
    static void positive(@Refinement("_ > 0") int x) {
    }

    static void negation(Settings settings) {
        if (!settings.enabled()) {
            positive(-1); // Expect: Refinement Error
        }
    }
}
