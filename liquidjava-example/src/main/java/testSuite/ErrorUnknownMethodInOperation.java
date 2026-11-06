package testSuite;

import liquidjava.specification.Refinement;

// Results of methods without refinements carry no information (issue #300)
@SuppressWarnings("unused")
public class ErrorUnknownMethodInOperation {
    public void lengthPlusOne(String s) {
        @Refinement("_ > 0")
        int x = s.length() + 1; // Expect: Refinement Error
    }

    static void take(@Refinement("_ > 0") int amount) {}

    static void takeTrue(@Refinement("_ == true") boolean flag) {}

    // also when passed straight as an argument (issue #451)
    public void lengthAsArgument(String s) {
        take(s.length()); // Expect: Refinement Error
    }

    public void isEmptyAsArgument(String s) {
        takeTrue(s.isEmpty()); // Expect: Refinement Error
    }
}
