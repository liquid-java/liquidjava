package testSuite.classes.delegated_constant_arguments_error;

import java.util.LinkedHashSet;

// LinkedHashSet() runs HashSet(16, .75f, true) and LinkedHashSet(int n) runs HashSet(n, .75f, true): the constants
// passed reach the spec as literals
public class Test {
    static void defaultCapacity() {
        LinkedHashSet<String> s = new LinkedHashSet<>();
        s.add("x");
    }

    static void linked() {
        LinkedHashSet<String> s = new LinkedHashSet<>();
        s.clear(); // Expect: State Refinement Error
    }

    static void otherCapacity(int n) {
        LinkedHashSet<String> s = new LinkedHashSet<>(n);
        s.add("x"); // Expect: State Refinement Error
    }
}
