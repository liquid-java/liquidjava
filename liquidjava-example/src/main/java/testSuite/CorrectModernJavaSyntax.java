package testSuite;

import java.io.StringReader;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectModernJavaSyntax {

    // local variable type inference (Java 10)
    void localVar() {
        var n = 5;
        @Refinement("_ > 0")
        int p = n;
    }

    // resource reference in try-with-resources (Java 9)
    void resourceReference() throws Exception {
        StringReader reader = new StringReader("a");
        try (reader) {
            reader.read();
        }
    }

    // switch expression (Java 14)
    int switchExpression(int k) {
        return switch (k) {
            case 0 -> 1;
            default -> 2;
        };
    }
}
