package testSuite.classes.inherited_overload_error;

import java.util.Stack;

public class UseStack {
    static void check() {
        Stack<String> stack = new Stack<>();
        stack.add("value");
        stack.remove(0);
        stack.peek(); // Expect: State Refinement Error
    }
}
