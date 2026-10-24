package testSuite;

import liquidjava.specification.Refinement;

public class ErrorUnprovableArgument {

    static void take(@Refinement("_ > 0") int amount) {}

    // nothing is known about amount, so its refinement can be neither violated nor proved
    static void use(int amount) {
        take(amount); // Expect: Refinement Error
    }
}
