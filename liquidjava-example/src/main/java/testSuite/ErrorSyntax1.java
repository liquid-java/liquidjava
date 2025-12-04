// Syntax Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorSyntax1 {
    public static void main(String[] args) {
        @Refinement("_ < 100 +")
        int value = 90 + 4;
    }
}
