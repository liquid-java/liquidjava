package testSuite;

import liquidjava.specification.Refinement;

public class CorrectChars {

    @Refinement("_ == 65")
    int getA() {
        return 'A';
    }

    void test() {
        printLetter('A');
        printLetter('Z');
        printLetter('a');
        printLetter('z');
    }

    void printLetter(@Refinement("_ >= 65 && _ <= 90 || _ >= 97 && _ <= 122") char c) {
        System.out.println(c);
    }
}
