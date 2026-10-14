package testSuite;

import org.example.missing.Item;

import liquidjava.specification.Refinement;

// A violation under a comparison of values whose types cannot be resolved is still reported (#388)
@SuppressWarnings("unused")
public class ErrorUnresolvedBinaryOperator {
    static void positive(@Refinement("_ > 0") int x) {
    }

    static void comparison(Item a, Item b) {
        if (a.id() == b.id()) {
            positive(0); // Expect: Refinement Error
        }
    }
}
