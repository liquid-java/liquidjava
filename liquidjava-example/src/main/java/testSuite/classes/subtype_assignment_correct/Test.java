package testSuite.classes.subtype_assignment_correct;

import java.io.IOException;
import liquidjava.specification.Refinement;

public class Test {
    static void load() throws IOException {
        throw new IOException("truncated");
    }

    static void knownState(@Refinement("noThrowable(_)") IOException original, Exception previous) {
        Exception failure = original;
        Exception copy = failure;
        copy.initCause(previous);
    }

    static void run(Exception previous) {
        Exception failure = null;
        try {
            load();
        } catch (IOException e) {
            failure = e;
        }
        if (failure != null) {
            Exception copy = failure;
            @Refinement("_ == 1") int checked = 1;
        }
    }
}
