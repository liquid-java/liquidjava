package testSuite.classes.inherited_overload_correct;

import java.util.Stack;

public class UseStack {
    static void check() {
        Stack<String> stack = new Stack<>();
        stack.add("value");
        stack.remove("value");
        stack.peek();
    }
}
