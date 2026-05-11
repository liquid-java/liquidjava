package testSuite;

import liquidjava.specification.Refinement;

public class CorrectOperatorAssignments {

    @Refinement("_ > 0")
    int plus(@Refinement("_ >= 0") int x) {
        x += 1; // x is now > 0
        return x;
    }

    @Refinement("_ > 0")
    int plusInvocation(@Refinement("_ >= 0") int x) {
        x += one(); // x is now > 0
        return x;
    }

    @Refinement("_ == 1")
    int one() {
        return 1;
    }

    @Refinement("_ < 0")
    int minus(@Refinement("_ <= 0") int x) {
        x -= 1; // x is now < 0
        return x;
    }

    @Refinement("_ >= 0")
    int multiply(int x) {
        x *= x; // x is now >= 0
        return x;
    }

    @Refinement("_ <= 1")
    int divide(@Refinement("0 <= _ && _ <= 3") int x) {
        x /= 2; // x is now <= 1
        return x;
    }

    @Refinement("_ == 0 || _ == 1")
    int remainder(@Refinement("_ >= 0") int x) {
        x %= 2; // x is now == 0 || x is now == 1
        return x;
    }

    int remainderInvocation(@Refinement("_ >= 0") int x) {
        @Refinement("_ == 10 || _ == 11")
        int y = 10;
        y += remainder(x); // x is now >= 6
        return x;
    }

    @Refinement("_ == 9")
    int plusUnaryInvocation() {
        int y = 10;
        y += -one();
        return y;
    }

    @Refinement("_ == 11")
    int plusConditional(@Refinement("_ >= 0") int x) {
        int y = 10;
        y += x >= 0 ? one() : 2;
        return y;
    }

    @Refinement("_ == 13")
    int plusBinaryExpression() {
        int y = 10;
        y += one() + 2;
        return y;
    }

    int plusArrayRead(int[] values) {
        int y = 10;
        y += values[0];
        return y;
    }

    @Refinement("_ == 11")
    int plusCast() {
        int y = 10;
        y += (int) one();
        return y;
    }
}
