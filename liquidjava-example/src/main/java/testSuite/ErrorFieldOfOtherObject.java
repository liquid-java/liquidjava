package testSuite;

import liquidjava.specification.Refinement;

// #322: other.val is a different object's field, not this.val
public class ErrorFieldOfOtherObject {
    @Refinement("val > 0")
    int val;

    public ErrorFieldOfOtherObject(@Refinement("_ > 0") int v) {
        val = v;
    }

    void read(ErrorFieldOfOtherObject other) {
        val = 7;
        @Refinement("_ == 7")
        int k = other.val; // Expect: Refinement Error
    }

    void sum(ErrorFieldOfOtherObject other) {
        val = 7;
        @Refinement("_ == 14")
        int k = val + other.val; // Expect: Refinement Error
    }

    void compound(ErrorFieldOfOtherObject other) {
        val = 7;
        int j = 0;
        j += other.val;
        @Refinement("_ == 7")
        int k = j; // Expect: Refinement Error
    }

    void condition(ErrorFieldOfOtherObject other) {
        val = 7;
        if (other.val == 3) {
            @Refinement("_ == 3")
            int k = val; // Expect: Refinement Error
        }
    }
}
