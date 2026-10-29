package testSuite.classes.delegated_subclass_sets_error;

import java.util.Stack;

// Stack() runs Vector(), whose spec sets only Vector's ghost: Stack's own state set starts at its default (empty)
public class Test {
    static void pushThenPeek() {
        Stack<String> s = new Stack<>();
        s.push("a");
        s.peek();
    }

    static void peekEmpty() {
        Stack<String> s = new Stack<>();
        s.peek(); // Expect: State Refinement Error
    }
}
