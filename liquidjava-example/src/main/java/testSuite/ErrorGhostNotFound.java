package testSuite;

import liquidjava.specification.Refinement;

public class ErrorGhostNotFound {

    public static void main(String[] args) {
        @Refinement("notFound(x)") // Expect: Not Found Error
        int x = 5;
    }
}
