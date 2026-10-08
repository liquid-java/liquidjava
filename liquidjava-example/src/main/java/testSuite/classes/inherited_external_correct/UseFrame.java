package testSuite.classes.inherited_external_correct;

import javax.swing.JFrame;

public class UseFrame {
    static void configure() {
        JFrame frame = new JFrame("example");
        frame.setUndecorated(true);
        frame.pack();
    }
}
