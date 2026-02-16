// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorChars {

    void test() {
        printLetter('$'); // error
    }

    void printLetter(@Refinement("_ >= 65 && _ <= 90 || _ >= 97 && _ <= 122") char c) {
        System.out.println(c);
    }
}
