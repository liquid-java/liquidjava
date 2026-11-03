package testSuite.classes.state_hints_same_simple_name_error;

import java.util.ArrayList;

public class Use {
    static void removeFromEmpty() {
        ArrayList<String> l = new ArrayList<>();
        l.remove(0); // Expect: State Refinement Error
    }
}
