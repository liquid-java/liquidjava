package testSuite;

import liquidjava.specification.Refinement;

public class CorrectFunctionsTutorial {

    // n + t1 overflows once the running sum exceeds Integer.MAX_VALUE. Bounding n to [0, 46340] keeps the
    // sum below 46340^2 < Integer.MAX_VALUE; the inductive ceiling _ <= n * 46340 (since n(n+1)/2 <= 46340*n
    // for n <= 46340) lets the modular check prove the addition cannot overflow at any recursion depth.
    @Refinement("_ >= 0 && _ >= n && _ <= n * 46340")
    public static int sum(@Refinement("0 <= n && n <= 46340") int n) {
        if (n <= 0) return 0;
        else {
            int t1 = sum(n - 1);
            return n + t1;
        }
    }

    // 0 - n overflows back to Integer.MIN_VALUE (still negative) when n == Integer.MIN_VALUE, so exclude it;
    // for every other n the magnitude is non-negative and at least n.
    @Refinement("_ >= 0 && _ >= n")
    public static int absolute(@Refinement("n > -2147483648") int n) {
        if (0 <= n) return n;
        else return 0 - n;
    }

    // From LiquidHaskell tutorial
    @Refinement("length(_) == length(vec1)")
    static int[] sumVectors(int[] vec1, @Refinement("length(vec1) == length(vec2)") int[] vec2) {
        int[] add = new int[vec1.length];
        if (vec1.length > 0) auxSum(add, vec1, vec2, 0);
        return add;
    }

    private static void auxSum(
            int[] add,
            int[] vec1,
            @Refinement("length(vec1) == length(vec2) && length(_) == length(add)") int[] vec2,
            @Refinement("_ >= 0 && _ < length(vec2)") int i) {
        add[i] = vec1[i] + vec2[i];
        if (i < add.length - 1) auxSum(add, vec1, vec2, i + 1);
    }
}
