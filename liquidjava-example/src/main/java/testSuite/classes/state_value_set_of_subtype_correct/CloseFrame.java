package testSuite.classes.state_value_set_of_subtype_correct;

import javax.swing.JFrame;

public class CloseFrame {
    static void close() {
        JFrame frame = new JFrame();
        if (frame.isDisplayable())
            frame.dispose();
    }
}
