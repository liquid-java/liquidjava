package testSuite.classes.state_hints_grandchild_spec_error;

import javax.swing.JFrame;

// JFrame has no spec: its state set comes from Window's, and setUndecorated from Frame's
public class Use {
    static void borderless() {
        JFrame window = new JFrame("Playfield");
        window.pack();
        window.setUndecorated(true); // Expect: State Refinement Error
    }
}
