package testSuite.classes.conflicting_ghost_names_correct;

import java.util.ArrayList;
import java.util.Stack;

public class SimpleTest {
    public void example() {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(10);
        list.get(0);

        Stack<Integer> stack = new Stack<>();
		if (stack.empty()) {
			stack.push(1);
            stack.peek();
		}
		stack.pop();
    }
}
