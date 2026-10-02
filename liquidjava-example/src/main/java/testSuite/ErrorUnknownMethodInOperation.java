package testSuite;

import liquidjava.specification.Refinement;

// Results of methods without refinements carry no information (issue #300)
@SuppressWarnings("unused")
public class ErrorUnknownMethodInOperation {
    public void lengthPlusOne(String s) {
        @Refinement("_ > 0")
        int x = s.length() + 1; // Expect: Refinement Error
    }
}
