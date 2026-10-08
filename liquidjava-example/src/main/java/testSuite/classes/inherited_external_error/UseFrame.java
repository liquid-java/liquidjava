package testSuite.classes.inherited_external_error;

import javax.swing.JFrame;

public class UseFrame {
    static void configure() {
        JFrame frame = new JFrame("example");
        frame.pack();
        frame.setUndecorated(true); // Expect: State Refinement Error
    }
}
