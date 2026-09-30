package testSuite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectNullLiterals {

    static void describe(String label, Object value) {
    }

    public static void main(String[] args) throws IOException {
        String name = null;
        if (name == null) {
            name = "default";
        }
        if (name != null) {
            describe(name, null);
        }

        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            out.write(1);
        } finally {
            if (out != null) {
                out.close();
            }
        }

        // refinements unrelated to the null literals are still checked
        @Refinement("x > 0")
        int x = 1;
        if (name != null) {
            @Refinement("y > 1")
            int y = x + 1;
        }
    }

    // facts next to a null comparison are kept
    static void conjunction(String s, int y) {
        if (s == null && y > 0) {
            @Refinement("_ > 0")
            int z = y;
        }
    }

    static void ternary(Object o) {
        @Refinement("_ == -1 || _ == 1")
        int x = (o == null) ? -1 : 1;
    }
}
