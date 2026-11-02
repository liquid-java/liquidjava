package testSuite.classes.found_field_state_error;

import java.util.Vector;

// the field's row holds its state, so Found keeps it under its source name (#457)
public class InterruptManager {
    private Vector<String> interrupts;

    public void initialize(int intrNumber) {
        interrupts = new Vector<String>(67);
        if (intrNumber < 0 || intrNumber >= 67) {
            return;
        }
        String handlers = interrupts.get(intrNumber); // Expect: State Refinement Error
    }
}
