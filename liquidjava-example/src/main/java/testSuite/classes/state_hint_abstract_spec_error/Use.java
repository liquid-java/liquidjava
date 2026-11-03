package testSuite.classes.state_hint_abstract_spec_error;

import java.io.IOException;
import java.io.InputStream;

public class Use {

    static int first(InputStream in) throws IOException {
        return in.read(); // Expect: State Refinement Error
    }
}
