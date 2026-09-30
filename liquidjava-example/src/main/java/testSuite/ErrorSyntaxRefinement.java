package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorSyntaxRefinement {
    public static void main(String[] args) {
        @Refinement("_ < 100 +") // Expect: Syntax Error
        int value = 90 + 4;
    }
}
