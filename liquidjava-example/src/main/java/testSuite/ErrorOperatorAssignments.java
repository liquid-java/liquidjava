package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorOperatorAssignments {

    @Refinement("_ == 1")
    int one() {
        return 1;
    }

    @Refinement("_ == 0 || _ == 1")
    int remainder(@Refinement("_ >= 0") int x) {
        x %= 2;
        return x;
    }

    @Refinement("_ == 12")
    int plusInvocation(@Refinement("_ >= 0") int x) {
        int y = 10;
        y += remainder(x);
        return y; // Refinement Error
    }

    @Refinement("_ == 10")
    int plusUnaryInvocation() {
        int y = 10;
        y += -one();
        return y; // Refinement Error
    }

    @Refinement("_ == 12")
    int plusConditional(@Refinement("_ >= 0") int x) {
        int y = 10;
        y += x >= 0 ? one() : 2;
        return y; // Refinement Error
    }

    @Refinement("_ == 14")
    int plusBinaryExpression() {
        int y = 10;
        y += one() + 2;
        return y; // Refinement Error
    }

    @Refinement("_ == 10")
    int plusArrayRead(int[] values) {
        int y = 10;
        y += values[0];
        return y; // Refinement Error
    }

    @Refinement("_ == 12")
    int plusCast() {
        int y = 10;
        y += (int) one();
        return y; // Refinement Error
    }
}
