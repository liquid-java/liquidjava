package testSuite.classes.state_hints_generic_inherited_method_error;

import java.util.Stack;

public class Use {
    static void popEmpty() {
        Stack<String> s = new Stack<>();
        s.pop(); // Expect: State Refinement Error
    }
}
