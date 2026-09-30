package testSuite;

import java.util.List;
import java.util.Map;

import liquidjava.specification.Refinement;

// Results of methods without refinements are unconstrained, so no branch is dead (issue #300)
@SuppressWarnings("unused")
public class ErrorUnknownMethodInComparison {
    interface Shape {
        int area();
    }

    int helper() {
        return 1;
    }

    public void thenBranch(String s) {
        if (s.length() < 3) {
            @Refinement("_ > 0")
            int y = -1; // Expect: Refinement Error
        }
    }

    public void elseBranch(String s) {
        if (s.length() < 3) {
        } else {
            @Refinement("_ > 0")
            int z = -1; // Expect: Refinement Error
        }
    }

    public void equalsInDisjunction(String s) {
        if (s.equals("a") || s.equals("b")) {
            @Refinement("_ > 0")
            int y = -1; // Expect: Refinement Error
        }
    }

    public void boxedResult(List<Integer> list) {
        if (list.get(0) > 3) {
        } else {
            @Refinement("_ > 0")
            int y = -1; // Expect: Refinement Error
        }
    }

    public void booleanResultInConjunction(Map<String, Integer> map, String k, int n) {
        if (map.containsKey(k) && n > 0) {
            @Refinement("_ > 0")
            int y = n - 1; // Expect: Refinement Error
        }
    }

    public void staticCall(int a, int b) {
        if (Math.max(a, b) > 0) {
        } else {
            @Refinement("_ > 0")
            int y = -1; // Expect: Refinement Error
        }
    }

    public void chainedCall(String s) {
        if (s.trim().length() > 0) {
            @Refinement("_ > 0")
            int y = -1; // Expect: Refinement Error
        }
    }

    public void implicitThisCall() {
        if (helper() > 0) {
        } else {
            @Refinement("_ > 0")
            int y = -1; // Expect: Refinement Error
        }
    }

    public void interfaceMethod(Shape shape) {
        if (shape.area() > 0) {
            @Refinement("_ > 0")
            int y = -1; // Expect: Refinement Error
        }
    }
}
