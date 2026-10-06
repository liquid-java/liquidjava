package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorPrimitiveNumbersTypes {
    @Refinement("_ < i && _ > 0")
    private static double fromType(@Refinement("_ > 0") int i) {
        return i * 0.1; // Expect: SMT Unknown Error
    }

    @Refinement(" _ < i && _ > 0")
    private static double fromType(@Refinement("_ > 0") long i) {
        return i * 0.1; // Expect: SMT Unknown Error
    }

    @Refinement(" _ < i && _ > 0")
    private static double fromType(@Refinement("_ > 0") short i) {
        return i * 0.1; // Expect: SMT Unknown Error
    }

    @Refinement("_ > i")
    private static float twice(@Refinement("i > 0") short i) {
        return i * 2f; // Expect: SMT Unknown Error
    }

    public static void main(String[] args) {
        @Refinement("_ > 5")
        int a = 10;

        @Refinement("_ > 5")
        long b = 100;

        @Refinement("_ > 5")
        short c = 10;

        @Refinement("_ > 5")
        float d = 7.4f;

        @Refinement("_ > 0")
        double e = fromType(a);
        e = fromType(b);
        e = fromType(c);

        @Refinement("f > 0")
        float f = twice(c);
    }
}
