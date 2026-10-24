package testSuite;

import liquidjava.specification.Refinement;

public class ErrorOutOfDomainArgument {

    static void take(@Refinement("_ > 0") int amount) {}

    static void use() {
        take(-1); // Expect: Refinement Error
    }
}
