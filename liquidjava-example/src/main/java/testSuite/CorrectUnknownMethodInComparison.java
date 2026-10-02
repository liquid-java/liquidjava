package testSuite;

import java.util.List;
import java.util.Map;

import liquidjava.specification.Refinement;

// Results of methods without refinements can be used in operations (issue #300)
@SuppressWarnings("unused")
public class CorrectUnknownMethodInComparison {
    interface Shape {
        int area();
    }

    int helper() {
        return 1;
    }

    public void lengthInComparison(String s) {
        if (s.length() < 3) {
            @Refinement("_ > 0")
            int y = 1;
        } else {
            @Refinement("_ > 0")
            int z = 1;
        }
    }

    public void equalsInDisjunction(String s) {
        boolean known = s.equals("a") || s.equals("b");
    }

    public void sizeInLoopBound(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
        }
    }

    public void boxedResult(List<Integer> list) {
        if (list.get(0) > 3) {
            @Refinement("_ > 0")
            int y = 1;
        }
    }

    public void booleanResultInConjunction(Map<String, Integer> map, String k, int n) {
        if (map.containsKey(k) && n > 0) {
            @Refinement("_ > 0")
            int y = n;
        }
    }

    public void staticCall(int a, int b) {
        if (Math.max(a, b) > 0) {
            @Refinement("_ > 0")
            int y = 1;
        }
    }

    public void chainedCall(String s) {
        if (s.trim().length() > 0) {
            @Refinement("_ > 0")
            int y = 1;
        }
    }

    public void implicitThisCall() {
        if (helper() > 0) {
            @Refinement("_ > 0")
            int y = 1;
        }
    }

    public void interfaceMethod(Shape shape) {
        if (shape.area() > 0) {
            @Refinement("_ > 0")
            int y = 1;
        }
    }
}
