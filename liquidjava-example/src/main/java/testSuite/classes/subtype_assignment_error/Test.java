package testSuite.classes.subtype_assignment_error;

import java.io.IOException;
import liquidjava.specification.Refinement;

public class Test {
    static void load() throws IOException {
        throw new IOException("truncated");
    }

    static void alreadyHasCause(@Refinement("withThrowable(_)") IOException original, Exception previous) {
        Exception failure = original;
        failure.initCause(previous); // Expect: State Refinement Error
    }

    static void run(Exception previous) {
        Exception failure = null;
        try {
            load();
        } catch (IOException e) {
            failure = e;
        }
        if (failure != null) {
            failure.initCause(previous); // Expect: State Refinement Error
        }
    }
}
