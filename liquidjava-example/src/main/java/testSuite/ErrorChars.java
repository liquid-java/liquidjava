package testSuite;

import liquidjava.specification.Refinement;

public class ErrorChars {
    static void printLetter(@Refinement("_ >= 65 && _ <= 90 || _ >= 97 && _ <= 122") char c) {
        System.out.println(c);
    }

    public static void main(String[] args) {
        printLetter('$'); // Refinement Error
    }
}
