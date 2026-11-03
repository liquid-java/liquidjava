package testSuite.classes.state_hints_redeclared_method_error;

import java.awt.Frame;

public class Use {
    static void borderless() {
        Frame host = new Frame("Playfield");
        host.pack();
        host.setUndecorated(true); // Expect: State Refinement Error
    }
}
