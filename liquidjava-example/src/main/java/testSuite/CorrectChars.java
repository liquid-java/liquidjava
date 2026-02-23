package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
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

    void test2() {
        @Refinement("_ == 97")
        char c = 'a';
        
        @Refinement("_ == 98")
        int res = inc(c);
    }

    @Refinement("_ == x + 1")
    int inc(int x) {
        return x + 1;
    }
}
