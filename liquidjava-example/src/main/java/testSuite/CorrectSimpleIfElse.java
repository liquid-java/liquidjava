package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectSimpleIfElse {

    // -Integer.MIN_VALUE overflows back to Integer.MIN_VALUE (still negative), so the negation is only
    // provably positive away from that boundary. We bound the input to a small negative window so the
    // result is a small positive value (also keeping the caller's `toPositive(ex_a) * 10` overflow-free).
    @Refinement("_ > 0 && _ < 1000")
    public static int toPositive(@Refinement("a < 0 && a > -1000") int a) {
        return -a;
    }

    @Refinement("_ < 0")
    public static int toNegative(@Refinement("a > 0") int a) {
        return -a;
    }

    public static void main(String[] args) {
        @Refinement("_ < 10")
        int a = 5;

        if (a < 0) {
            @Refinement("b < 0")
            int b = a;
        } else {
            @Refinement("b >= 0")
            int b = a;
        }

        // EXAMPLE 2
        // Bound ex_a to the small negative window toPositive accepts (and keep the *10 below in range).
        @Refinement("_ < 10 && _ > -1000")
        int ex_a = 5;
        if (ex_a < 0) {
            @Refinement("_ >= 10")
            int ex_b = toPositive(ex_a) * 10;
        } else {
            if (ex_a != 0) {
                @Refinement("_ < 0")
                int ex_d = toNegative(ex_a);
            }
            @Refinement("_ < ex_a")
            int ex_c = -10;
        }
    }
}
