package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorNullLiterals {

    // comparisons with null are unknown booleans, so no branch may be considered unreachable

    static void elseBranch(String name) {
        if (name == null) {
            System.out.println("none");
        } else {
            @Refinement("_ > 0")
            int x = -1; // Expect: Refinement Error
        }
    }

    static void elseBranchOfDisjunction(String name, int y) {
        if (name == null || y > 0) {
            System.out.println("some");
        } else {
            @Refinement("_ > 0")
            int x = -1; // Expect: Refinement Error
        }
    }

    static void elseBranchOfConjunction(String name, int y) {
        if (name != null && y > 0) {
            System.out.println("some");
        } else {
            @Refinement("_ <= 0")
            int z = y; // Expect: Refinement Error
        }
    }

    static void ternary(Object o) {
        @Refinement("_ < 0")
        int x = (o == null) ? -1 : 1; // Expect: Refinement Error
    }

    static void booleanValue(Object o) {
        boolean isNull = o == null;
        @Refinement("_ == true")
        boolean b = isNull; // Expect: Refinement Error
    }
}
