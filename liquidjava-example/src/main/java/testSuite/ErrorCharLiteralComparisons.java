package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorCharLiteralComparisons {

    @Refinement("_ == 0")
    static int zero(char c) {
        if (c == '0')
            return c; // Expect: Refinement Error
        return 0;
    }

    @Refinement("_ == 5")
    static int letterA(char c) {
        int a = 5;
        if (c == 'a')
            return c; // Expect: Refinement Error
        return 5;
    }
}
