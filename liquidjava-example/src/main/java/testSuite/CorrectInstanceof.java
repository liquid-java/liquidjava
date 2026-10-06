package testSuite;

import liquidjava.specification.Refinement;

public class CorrectInstanceof {

    static int positive(@Refinement("_ > 0") int x) {
        return x;
    }

    static String describe(Object o) {
        if (o instanceof String) {
            return "text";
        }
        return "other";
    }

    static int size(Object o, @Refinement("_ > 0") int n) {
        boolean isText = o instanceof String;
        if (isText && n > 1) {
            return positive(n - 1);
        }
        if (!(o instanceof Integer) || n > 3) {
            return positive(n);
        }
        return positive(n + 1);
    }
}
